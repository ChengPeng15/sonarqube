package org.sonar.server.user.ws;

import org.sonar.db.user.UserDto;
import org.sonarqube.ws.Users;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public class DefaultHomepageFinder {

  public Users.CurrentWsResponse.Homepage findFor(UserDto user) {
    if (homepageIsSetOn(user)) {
      return homepageOf(user);
    } else {
      return newDefaultHomepageOf(user.getId());
    }
  }

  private Users.CurrentWsResponse.Homepage homepageOf(UserDto user) {
    return Users.CurrentWsResponse.Homepage.newBuilder()
      .setType(user.getHomepageType())
      .setKey(user.getHomepageKey())
      .build();
  }

  private Users.CurrentWsResponse.Homepage newDefaultHomepageOf(Integer userId) {
    return Users.CurrentWsResponse.Homepage.newBuilder()
      .setType("my-projects")
      .setKey("")
      .build();
  }

  private boolean homepageIsSetOn(UserDto user) {
    return isNotBlank(user.getHomepageType());
  }

}
