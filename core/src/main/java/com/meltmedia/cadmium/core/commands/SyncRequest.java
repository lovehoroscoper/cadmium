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
package com.meltmedia.cadmium.core.commands;

public class SyncRequest extends AbstractMessageBody {
  protected GitLocation contentLocation;
  protected GitLocation configLocation;
  protected String repo;

  protected String branch;
  protected String sha;
  protected String configRepo;
  protected String configBranch;
  protected String configSha;
  private String comment;
  public void setComment(String comment) {
    this.comment = comment;
  }
  public String getComment() {
    return comment;
  }
  public GitLocation getContentLocation() {
    return contentLocation;
  }
  public void setContentLocation(GitLocation contentLocation) {
    this.contentLocation = contentLocation;
  }
  public GitLocation getConfigLocation() {
    return configLocation;
  }
  public void setConfigLocation(GitLocation configLocation) {
    this.configLocation = configLocation;
  }
}
