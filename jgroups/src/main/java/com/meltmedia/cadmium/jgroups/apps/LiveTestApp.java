package com.meltmedia.cadmium.jgroups.apps;

import java.io.File;
import java.net.URL;
import java.util.Map;

import org.jgroups.JChannel;
import org.jgroups.Message;

import com.meltmedia.cadmium.jgroups.ContentService;
import com.meltmedia.cadmium.jgroups.CoordinatedWorker;
import com.meltmedia.cadmium.jgroups.CoordinatedWorkerListener;
import com.meltmedia.cadmium.jgroups.receivers.UpdateChannelReceiver;

public class LiveTestApp implements CoordinatedWorker, ContentService {
  
  private CoordinatedWorkerListener listener;

  @Override
  public void beginPullUpdates(Map<String, String> properties) {
    new Thread(new Runnable() {
      public void run() {
        System.out.println("Pulling Updates!!!");
        System.out.print("Hit enter when you are ready to move on or enter the word 'kill' to fail the update! ");
        try {
          String response = readLine();
          if(response != null && response.trim().equalsIgnoreCase("kill")) {
            listener.workFailed();
            return;
          }
        } catch(Exception e) {}
        System.out.println("Waiting!!!");
        listener.workDone(null);
      }
    }).start();
  }

  @Override
  public void switchContent(File newDir) {
    new Thread(new Runnable() {
      public void run() {
        System.out.println("Switching content!!!!");
      }
    }).start();
  }

  @Override
  public void killUpdate() {
    new Thread(new Runnable() {
      public void run() {
        System.out.println("Killing Update!!!!");
      }
    }).start();
  }

  @Override
  public void setListener(CoordinatedWorkerListener listener) {
    this.listener = listener;
  }
  
  public synchronized String readLine() throws Exception {
    String cmd = System.console().readLine();
    return cmd;
  }

  /**
   * @param args
   * @throws Exception 
   */
  public static void main(String[] args) throws Exception {
    URL propsUrl = LiveTestApp.class.getClassLoader().getResource("tcp.xml");
    String configFile = propsUrl.toString();
    System.out.println("Here is the configuration : {"+configFile+"}");
    JChannel cnl = new JChannel(propsUrl);
    cnl.connect("JGroupsContentUpdateTestingChannel");
    LiveTestApp worker = new LiveTestApp();
    UpdateChannelReceiver receiver = new UpdateChannelReceiver(cnl, worker, worker);
    if(receiver.getMyState() == UpdateChannelReceiver.UpdateState.IDLE){
      System.out.print("Hit enter to continue: ");
      worker.readLine();
      if(receiver.getMyState() == UpdateChannelReceiver.UpdateState.IDLE){
        cnl.send(new Message(null, null, "UPDATE"));
      }
    }
    while(true) {
      try{
        Thread.sleep(1000l);
      } catch(Exception e){}
      if(receiver.getMyState() == UpdateChannelReceiver.UpdateState.IDLE) {
        break;
      }
    }
    
  }

}
