package com.meltmedia.cadmium.jgroups;

import java.io.File;

public interface ContentService {
  public void switchContent(String newDir);
  public void setListener(ContentServiceListener listener);
}