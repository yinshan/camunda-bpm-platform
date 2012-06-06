package com.camunda.fox.platform.tasklist;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.activiti.engine.TaskService;

import com.camunda.fox.platform.tasklist.event.SignOutEvent;
import com.camunda.fox.platform.tasklist.event.TaskNavigationLinkSelectedEvent;
import com.camunda.fox.platform.tasklist.identity.FoxIdentityService;

@Named
@ViewScoped
public class TaskNavigation implements Serializable {

  private static final Logger log = Logger.getLogger(TaskNavigation.class.getSimpleName());

  private static final long serialVersionUID = 1L;

  @Inject
  private Identity identity;

  @Inject
  TaskService taskService;

  @Inject
  FoxIdentityService foxIdentityService;

  @Inject
  private Event<TaskNavigationLinkSelectedEvent> taskNavigationLinkSelectedEvent;

  private MyTasksLink myTasksLink;
  private UnassignedTasksLink unassignedTasksLink;
  private List<GroupTasksLink> groupTasksLinks;
  private List<ColleaguesTasksLink> colleaguesTasksLinks;

  private TaskNavigationLink selected;

  @PostConstruct
  protected void init() {
    log.info("initializing " + this.getClass().getSimpleName() + " (" + this + ")");
    selected = getMyTasksLink();
    selected.setActive(true);
  }

  public MyTasksLink getMyTasksLink() {
    if (myTasksLink == null) {
      long personalTasksCount = taskService.createTaskQuery().taskAssignee(identity.getCurrentUser().getUsername()).count();
      myTasksLink = new MyTasksLink("My Tasks (" + personalTasksCount + ")", false);
    }
    return myTasksLink;
  }

  public UnassignedTasksLink getUnassignedTasksLink() {
    if (unassignedTasksLink == null) {
      long unassignedTasksCount = taskService.createTaskQuery().taskCandidateUser(identity.getCurrentUser().getUsername()).count();
      unassignedTasksLink = new UnassignedTasksLink("Unassigned Tasks (" + unassignedTasksCount + ")", false);
    }
    return unassignedTasksLink;
  }

  public List<GroupTasksLink> getGroupTasksLinks() {
    if (groupTasksLinks == null) {
      groupTasksLinks = new ArrayList<GroupTasksLink>();
      List<String> groups = foxIdentityService.getGroupsByUserId(identity.getCurrentUser().getUsername());
      for (String groupId : groups) {
        long groupTasksCount = taskService.createTaskQuery().taskCandidateGroup(groupId).count();
        GroupTasksLink gourpLink = new GroupTasksLink(groupId + " (" + groupTasksCount + ")", groupId, false);
        groupTasksLinks.add(gourpLink);
      }
    }
    return groupTasksLinks;
  }

  public List<ColleaguesTasksLink> getColleaguesTasksLinks() {
    if (colleaguesTasksLinks == null) {
      colleaguesTasksLinks = new ArrayList<ColleaguesTasksLink>();
      List<com.camunda.fox.platform.tasklist.identity.User> colleagues = foxIdentityService.getColleaguesByUserId(identity.getCurrentUser().getUsername());
      for (com.camunda.fox.platform.tasklist.identity.User colleague : colleagues) {
        long colleagueTasksCount = taskService.createTaskQuery().taskAssignee(colleague.getUsername()).count();
        ColleaguesTasksLink colleaguesLink = new ColleaguesTasksLink(colleague.getFirstname() + " " + colleague.getLastname() + " (" + colleagueTasksCount
                + ")", colleague.getUsername(), false);
        colleaguesTasksLinks.add(colleaguesLink);
      }
    }
    return colleaguesTasksLinks;
  }

  public void select(@Observes TaskNavigationLinkSelectedEvent taskNavigationLinkSelectedEvent) {
    if (!taskNavigationLinkSelectedEvent.getLink().equals(selected)) {
      if (selected != null) {
        selected.setActive(false);
      }
      selected = taskNavigationLinkSelectedEvent.getLink();
      selected.setActive(true);
    }
  }

  public void reset(@SuppressWarnings("unused") @Observes SignOutEvent signOutEvent) {
    myTasksLink = null;
    unassignedTasksLink = null;
    groupTasksLinks = null;
  }

  public void select(TaskNavigationLink link) {
    log.fine("Menu entry " + link + " was selected, firing TaskNavigationLinkSelectedEvent.");
    taskNavigationLinkSelectedEvent.fire(new TaskNavigationLinkSelectedEvent(link));
  }

}