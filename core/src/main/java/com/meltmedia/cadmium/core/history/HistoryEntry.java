package com.meltmedia.cadmium.core.history;

import java.util.Date;

public class HistoryEntry {
  private Date timestamp;
  private String branch;
  private String revision;
  private long timeLive;
  private String openId;
  private String servedDirectory;
  private boolean revertible;
  private String comment;
  
  public HistoryEntry(){}
  
  public HistoryEntry(Date timestamp, String branch, String revision, long timeLive, String openId, String servedDirectory, boolean revertible, String comment) {
    this.timestamp = timestamp;
    this.branch = branch;
    this.revision = revision;
    this.timeLive = timeLive;
    this.openId = openId;
    this.servedDirectory = servedDirectory;
    this.revertible = revertible;
    this.comment = comment;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public String getBranch() {
    return branch;
  }

  public void setBranch(String branch) {
    this.branch = branch;
  }

  public String getRevision() {
    return revision;
  }

  public void setRevision(String revision) {
    this.revision = revision;
  }

  public long getTimeLive() {
    return timeLive;
  }

  public void setTimeLive(long timeLive) {
    this.timeLive = timeLive;
  }

  public String getOpenId() {
    return openId;
  }

  public void setOpenId(String openId) {
    this.openId = openId;
  }

  public String getServedDirectory() {
    return servedDirectory;
  }

  public void setServedDirectory(String servedDirectory) {
    this.servedDirectory = servedDirectory;
  }

  public boolean isRevertible() {
    return revertible;
  }

  public void setRevertible(boolean revertible) {
    this.revertible = revertible;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }
}