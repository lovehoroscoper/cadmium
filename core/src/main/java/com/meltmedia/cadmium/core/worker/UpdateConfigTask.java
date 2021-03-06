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
package com.meltmedia.cadmium.core.worker;

import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.eclipse.jgit.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meltmedia.cadmium.core.commands.ContentUpdateRequest;
import com.meltmedia.cadmium.core.commands.GitLocation;
import com.meltmedia.cadmium.core.config.ConfigManager;
import com.meltmedia.cadmium.core.git.DelayedGitServiceInitializer;
import com.meltmedia.cadmium.core.git.GitService;

public abstract class UpdateConfigTask implements Callable<Boolean> {
  private final Logger log = LoggerFactory.getLogger(getClass());
  
  private DelayedGitServiceInitializer service;
  private ConfigManager configManager;
  private Future<Boolean> previousTask;
  private String prefix = "";
  private String type = "content.";

  private ContentUpdateRequest body;
  
  public UpdateConfigTask(String prefix, DelayedGitServiceInitializer service, ContentUpdateRequest body, ConfigManager manager, Future<Boolean> previousTask) {
    this.service = service;
    this.body = body;
    this.configManager = manager;
    this.previousTask = previousTask;
    if(!StringUtils.isEmptyOrNull(prefix)){
      if(!prefix.endsWith(".")) {
        prefix = prefix + ".";
      }
      this.prefix = prefix;
      this.type = prefix;
    }
  }

  @Override
  public Boolean call() throws Exception {
    final GitService service = this.service.getGitService();
    Properties configProperties = configManager.getDefaultProperties();
    
    try {
      final String branch = configProperties.getProperty(prefix + "branch");
      final String revision = service.isTag(branch) ? null : configProperties.getProperty(prefix + "git.ref.sha");
      try {
        if(previousTask != null) {
          Boolean lastResponse = previousTask.get();
          if(lastResponse != null && !lastResponse.booleanValue() ) {
            throw new ExecutionException("Previous task failed", new Exception());
          }
        }
        log.info("Updating config.properties file");
        String lastUpdatedDir = getNextDirectory();
                        
        if(configProperties.containsKey("com.meltmedia.cadmium."+prefix+"lastUpdated")) {
          configProperties.setProperty("com.meltmedia.cadmium."+prefix+"previous", configProperties.getProperty("com.meltmedia.cadmium."+prefix+"lastUpdated"));
        }
        configProperties.setProperty("com.meltmedia.cadmium."+prefix+"lastUpdated", lastUpdatedDir);
        if(configProperties.containsKey(prefix + "branch")) {
          configProperties.setProperty(prefix + "branch.last", configProperties.getProperty(prefix + "branch"));
        }
        configProperties.setProperty(prefix + "branch", service.getBranchName());
        if(configProperties.containsKey(prefix + "git.ref.sha")) {
          configProperties.setProperty(prefix + "git.ref.sha.last", configProperties.getProperty(prefix + "git.ref.sha"));
        }
        configProperties.setProperty(prefix + "git.ref.sha", service.getCurrentRevision());
        
        configProperties.setProperty(prefix + "repo", service.getRemoteRepository());
        
        // NOTE: It is hard to tell if this condition will happen.  Just being defensive.
        if( body.getContentLocation() == null ) body.setContentLocation(new GitLocation());
        body.getContentLocation().setRepository(service.getRemoteRepository());
        body.getContentLocation().setBranch(service.getBranchName());
        body.getContentLocation().setRevision(service.getCurrentRevision());
        
        if(configProperties.containsKey("updating."+type+"to.sha")) {
          configProperties.remove("updating."+type+"to.sha");
        }
        if(configProperties.containsKey("updating."+type+"to.branch")) {
          configProperties.remove("updating."+type+"to.branch");
        }
        
        try{
          configManager.persistDefaultProperties();
        } catch(Exception e) {
          log.warn("Failed to write out config file", e);
        }
        
        return true;
      } catch(ExecutionException e) {
        new Timer().schedule(new TimerTask() {
          public void run() {
            try {
              log.info("Reverting to last branch["+branch+"] and revision ["+revision+"]!");
              if(!service.getBranchName().equals(branch)) {
                service.switchBranch(branch);
              }
              if(revision != null && !service.getCurrentRevision().equals(revision)) {
                service.resetToRev(revision);
              }
            } catch(Exception e1) {
              log.error("Failed to revert", e1);
            }
          }
        }, 250l);
        throw e;
      }
    } finally {
      this.service.releaseGitService();
    }
  }
  
  public abstract String getNextDirectory();

}
