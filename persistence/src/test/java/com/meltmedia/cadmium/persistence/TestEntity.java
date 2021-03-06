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
package com.meltmedia.cadmium.persistence;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Test JPA Entity to test this module with.
 * 
 * @author John McEntire
 *
 */
@Entity
public class TestEntity implements Serializable {

  private static final long serialVersionUID = -4298148780334251936L;
  private Long id = null;
  private String name = null;
  private Boolean flag = null;
  private Set<Integer> ints = null;
  private Date created = null;
  
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  public Long getId() {
    return id;
  }
  
  public void setId(Long id) {
    this.id = id;
  }
  
  @Basic
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  @Basic
  public Boolean getFlag() {
    return flag;
  }
  
  public void setFlag(Boolean flag) {
    this.flag = flag;
  }
  
  @ElementCollection
  public Set<Integer> getInts() {
    return ints;
  }
  
  public void setInts(Set<Integer> ints) {
    this.ints = ints;
  }
  
  @Temporal(TemporalType.TIMESTAMP)
  public Date getCreated() {
    return created;
  }
  
  public void setCreated(Date created) {
    this.created = created;
  }
  
}
