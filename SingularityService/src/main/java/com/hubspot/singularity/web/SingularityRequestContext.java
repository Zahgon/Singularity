package com.hubspot.singularity.web;

/**
 * The part of a request the service reads when it works out which user it is answering for.
 */
public interface SingularityRequestContext {
  /**
   * The value the request carries under this name. A request that carries the name more than once answers
   * every value it carries, separated by commas, which is what the service has always read.
   */
  String getHeaderString(String name);

  /** The first value the query carries under this name, or nothing when the query does not carry it. */
  String getFirstQueryParameter(String name);
}
