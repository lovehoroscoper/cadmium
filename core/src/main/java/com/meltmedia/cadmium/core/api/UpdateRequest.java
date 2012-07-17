package com.meltmedia.cadmium.core.api;

public class UpdateRequest {
  private String branch;
  private String sha;
  private String comment;
  
  public UpdateRequest() {}

  public String getBranch() {
    return branch;
  }

  public void setBranch(String branch) {
    this.branch = branch;
  }

  public String getSha() {
    return sha;
  }

  public void setSha(String sha) {
    this.sha = sha;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }
}
