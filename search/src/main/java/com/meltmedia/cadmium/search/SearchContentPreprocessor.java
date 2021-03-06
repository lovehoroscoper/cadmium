/**
 *    Copyright 2012 meltmedia
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.meltmedia.cadmium.search;

import com.google.inject.Inject;
import com.meltmedia.cadmium.core.meta.ConfigProcessor;
import jodd.jerry.Jerry;
import jodd.lagarto.dom.Node;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

@Singleton
public class SearchContentPreprocessor  implements ConfigProcessor, IndexSearcherProvider, Closeable {
  private final Logger log = LoggerFactory.getLogger(getClass());
  
  @Inject(optional=true)
  protected Set<SearchPreprocessor> searchPreprocessors;
  
  
  public static FileFilter HTML_FILE_FILTER = new FileFilter() {
    @Override
    public boolean accept(File pathname) {
      return pathname.isFile() 
          && pathname.getPath().toLowerCase().matches(".*\\.htm[l]?\\Z") 
          && !pathname.getName().toLowerCase().matches("^((\\d{3})|(\\d{2}[x])|(\\d[x]{2}))\\.htm[l]?$");
    }
  };
  
  public static FileFilter DIR_FILTER = new FileFilter() {
    @Override
    public boolean accept(File pathname) {
      return pathname.isDirectory();
    }
  };
  
  public static FileFilter NOT_INF_DIR_FILTER = new FileFilter() {
    @Override
    public boolean accept(File pathname) {
      return pathname.isDirectory() && !pathname.getName().endsWith("-INF");
    }
  };
  
  public static Comparator<File> FILE_NAME_COMPARATOR = new Comparator<File>() {
    @Override
    public int compare(File file1, File file2) {
      return file1.getName().compareTo(file2.getName());
    }
  };
  
  /**
   * A template class that scans the content directory, starting at the root, and
   * calls scan(File) for every file that matches the provided content filter.
   * 
   * @author Christian Trimble
   */
  public static abstract class ContentScanTemplate
  {
    private FileFilter contentFilter;

    public ContentScanTemplate(FileFilter contentFilter) {
      this.contentFilter = contentFilter;
    }
    
    public void scan( File contentRoot ) throws Exception {
      // create the frontier and add the content root.
      LinkedList<File> frontier = new LinkedList<File>();
      
      // scan the content root dir for html files.
      for( File htmlFile : contentRoot.listFiles(contentFilter)) {
        handleFile(htmlFile);
      }
      
      // add the non "-INF" directories, in a predictable order.
      frontier.subList(0, 0).addAll(Arrays.asList(sort(contentRoot.listFiles(NOT_INF_DIR_FILTER), FILE_NAME_COMPARATOR)));
      
      while( !frontier.isEmpty() ) {
        File dir = frontier.removeFirst();
        
        // scan the html files in the directory.
        for( File htmlFile : dir.listFiles(contentFilter)) {
          handleFile(htmlFile);
        }
   
        // add the directories, in a predictable order.
        frontier.subList(0, 0).addAll(Arrays.asList(sort(dir.listFiles(DIR_FILTER), FILE_NAME_COMPARATOR)));
      }
    }
    
    /**
     * An call to Arrays.sort(array, comparator) that returns the array argument after the sort.
     * 
     * @param array the array to sort.
     * @param comparator the comparator to sort with.
     * @return the array argument.
     */
    private static <T> T[] sort( T[] array, Comparator<T> comparator ) {
      Arrays.sort(array, comparator);
      return array;
    }
    
    public abstract void handleFile( File file )
      throws Exception;
  }
  
  private File indexDir;
  private File dataDir;
  private SearchHolder liveSearch = null;
  private SearchHolder stagedSearch = null;
  private static Analyzer analyzer = new CadmiumAnalyzer(Version.LUCENE_43);
  private final ReentrantReadWriteLock locker = new ReentrantReadWriteLock();
  private final ReadLock readLock = locker.readLock();
  private final WriteLock writeLock = locker.writeLock();
  

  @Override
  public synchronized void processFromDirectory(String metaDir) throws Exception {
    SearchHolder newStagedSearcher = new SearchHolder();
    indexDir = new File(metaDir, "lucene-index");
    dataDir = new File(metaDir).getParentFile();
    newStagedSearcher.directory = new NIOFSDirectory(indexDir);
    IndexWriter iwriter = null;
    try {
      iwriter = new IndexWriter(newStagedSearcher.directory, new IndexWriterConfig(Version.LUCENE_43, analyzer).setRAMBufferSizeMB(5));
      iwriter.deleteAll();
      writeIndex(iwriter, dataDir);
    }
    finally {
      IOUtils.closeQuietly(iwriter);
      iwriter = null;
    }
    newStagedSearcher.indexReader = DirectoryReader.open(newStagedSearcher.directory);
    SearchHolder oldStage = stagedSearch;
    stagedSearch = newStagedSearcher;
    if(oldStage != null) {
      oldStage.close();
    }
    log.info("About to call processSearchPreprocessors()");
    processSearchPreprocessors(newStagedSearcher.indexReader, analyzer, "content");
  }
  
  void writeIndex( final IndexWriter indexWriter, File contentDir ) throws Exception {
    new ContentScanTemplate(HTML_FILE_FILTER) {

      private Jerry.JerryParser jerryParser = null;

      @Override
      public void handleFile(File file) throws Exception {
        try {
          if(jerryParser == null) {
            jerryParser = Jerry.jerry().enableHtmlMode();
            jerryParser.getDOMBuilder().setCaseSensitive(false);
            jerryParser.getDOMBuilder().setParseSpecialTagsAsCdata(true);
            jerryParser.getDOMBuilder().setSelfCloseVoidTags(false);
            jerryParser.getDOMBuilder().setConditionalCommentExpression(null);
            jerryParser.getDOMBuilder().setEnableConditionalComments(false);
            jerryParser.getDOMBuilder().setImpliedEndTags(false);
            jerryParser.getDOMBuilder().setIgnoreComments(true);
          }
          String htmlContent = FileUtils.readFileToString(file, "UTF-8");
          Jerry jerry = jerryParser.parse(htmlContent);
          
          // if we should not index this file, move on.
          if(!shouldIndex(jerry)) return;

          String title = jerry.$("html > head > title").text();

          Jerry removals = jerry.$("title,head,script,[cadmium=\"no-index\"]");
          if(removals.size() > 0) {
          	log.debug("Removing {} element[s]", removals.length());
          	removals.remove();
          } else {
            log.debug("No elements to remove");
          }

          String textContent = jerry.$("body").text();

  	      Document doc = new Document();
  	      doc.add(new TextField("title", title, Field.Store.YES));
  	      doc.add(new TextField("content", textContent, Field.Store.YES));
  	      doc.add(new TextField("path", file.getPath().replaceFirst(dataDir.getPath(), ""), Field.Store.YES));
  	      indexWriter.addDocument(doc);
        } catch(Throwable t) {
          log.warn("Failed to index page ["+file+"]", t);
        }

      }
    }.scan(contentDir); 
    
  }

  @Override
  public synchronized void makeLive() {
  	log.info("About to call lock on writeLock");
    writeLock.lock();
    if( this.stagedSearch != null && this.stagedSearch.directory != null && this.stagedSearch.indexReader != null ) {
    	log.info("About to call makeLiveProcessSearchPreprocessors()");
    	makeLiveProcessSearchPreprocessors();
    	SearchHolder oldLive = liveSearch;
      liveSearch = stagedSearch;
      IOUtils.closeQuietly(oldLive);
      stagedSearch = null;
    }
    writeLock.unlock();
  }
  
  public void finalize() {
    IOUtils.closeQuietly(liveSearch);
    IOUtils.closeQuietly(stagedSearch);
 }

  @Override
  public IndexSearcher startSearch() {
    readLock.lock();
    if(this.liveSearch != null) {
      if(this.liveSearch.indexSearcher == null) {
        IndexSearcher searcher = new IndexSearcher(this.liveSearch.indexReader);
        this.liveSearch.indexSearcher = searcher;
      }
      return this.liveSearch.indexSearcher;
    }
    return null;
  }

  @Override
  public void endSearch() {
    readLock.unlock();
  }

  @Override
  public Analyzer getAnalyzer() {
    return analyzer;
  }
  
  public File getIndexDir() {
    return indexDir;
  }

  public File getDataDir() {
    return dataDir;
  }

  private class SearchHolder implements Closeable {
    private Directory directory = null;
    private IndexReader indexReader = null;
    private IndexSearcher indexSearcher = null;
    public void close() {
      IOUtils.closeQuietly(indexReader);
      IOUtils.closeQuietly(directory);
    }
    public void finalize() {
      close();
    }
  }

  @Override
  public void close() throws IOException {
    finalize();
  }
  
  /**
   * Returns true if an html file should be indexed, false otherwise.  Currently, this only tests for the existance of a robots meta tag with a
   * content value containing "noindex".
   * 
   * @param jerry the Jerry context for the html page to test.
   * @return
   */
  private static boolean shouldIndex(Jerry jerry) {
    Jerry metaTags = jerry.$("html > head > meta");
    if(metaTags.get().length > 0) {
      for(Node $this : metaTags.get()){
          if($this.hasAttribute("name") && "robots".equals($this.getAttribute("name").toLowerCase()) && $this.getAttribute("content") != null) {
            String contentValue = $this.getAttribute("content");
            if(contentValue == null || contentValue.toLowerCase().contains("noindex")) {
              return false;
            }
          }
      }
    }
    return true;
  }
  
  protected void processSearchPreprocessors(IndexReader reader, Analyzer analyzer, String field) {
  	
  	log.info("processing search preprocessors.");
  	log.info("preprocessors to process: {}", searchPreprocessors);
  	if(searchPreprocessors != null) {  		
  		for(SearchPreprocessor p : searchPreprocessors) {  			
  			try {
  				log.info("Processing: {}");
					p.process(reader, analyzer, field);
				} 
  			catch (Exception e) {
					
  				log.warn("Problem setting up search suggester preprocessor for field: {}", field);
				}
  		}
  	}
  }
  
  protected void makeLiveProcessSearchPreprocessors() {
  	  	
  	log.info("Making live search preprocessors.");
  	log.info("preprocessors to process: {}", searchPreprocessors);
  	if(searchPreprocessors != null) {  		
  		for(SearchPreprocessor p : searchPreprocessors) {  			
  			try {
					p.makeLive();
				} 
  			catch (Exception e) {
					
  				log.warn("Problem making live the search preprocessor");
				}
  		}
  	}
  }

}
