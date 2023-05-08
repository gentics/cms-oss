package com.gentics.contentnode.tests.rest.group;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.attribute;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.exception.DuplicateValueException;
import com.gentics.contentnode.exception.MissingFieldException;
import com.gentics.contentnode.exception.MovingGroupNotPossibleException;
import com.gentics.contentnode.exception.UserWithoutGroupException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.rest.model.Group;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.model.response.GroupList;
import com.gentics.contentnode.rest.model.response.GroupLoadResponse;
import com.gentics.contentnode.rest.resource.impl.GroupResourceImpl;
import com.gentics.contentnode.rest.resource.impl.UserResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.tests.utils.Expected;

/**
 * Test cases for updating groups
 */
public class GroupEditTest extends AbstractGroupEditTest {
	/**
	 * Test creating a new group (as subgroup of node group)
	 * @throws NodeException
	 */
	@Test
	public void testCreate() throws NodeException {
		String name = "New Group";
		String description = "Group description";
		GroupResourceImpl resource = new GroupResourceImpl();
		Group created = Trx.supply(() -> {
			GroupLoadResponse response = resource.add(String.valueOf(NODE_GROUP), new Group().setName(name).setDescription(description));
			ContentNodeRESTUtils.assertResponseOK(response);

			return response.getGroup();
		});

		// assert response
		assertThat(created).as("Created group")
			.has(attribute("name", name))
			.has(attribute("description", description));

		// assert group created in correct mother group
		List<Group> groups = Trx.supply(() -> {
			GroupList response = resource.subgroups(String.valueOf(NODE_GROUP), null, null, null, null);
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getItems();
		});
		assertThat(groups).as("Subgroups of node group").usingFieldByFieldElementComparator().containsOnly(created);

		// assert permissions duplicated
		Set<Triple<Integer, Integer, String>> nodeGroupPerms = Trx.supply(() -> ContentNodeTestUtils.getGroupPerms(NODE_GROUP));
		Set<Triple<Integer, Integer, String>> groupPerms = Trx.supply(() -> ContentNodeTestUtils.getGroupPerms(created.getId()));
		assertThat(groupPerms).as("Created group permissions").containsOnlyElementsOf(nodeGroupPerms);
	}

	/**
	 * Test that creating groups with duplicate names fails
	 * @throws NodeException
	 */
	@Test
	@Expected(ex = DuplicateValueException.class, message = "Das Feld 'Name' darf nicht den Wert 'New Group' haben, weil dieser Wert bereits verwendet wird.")
	public void testCreateDuplicate() throws NodeException {
		String name = "New Group";
		String description = "Group description";
		GroupResourceImpl resource = new GroupResourceImpl();
		Trx.supply(() -> {
			GroupLoadResponse response = resource.add(String.valueOf(NODE_GROUP), new Group().setName(name).setDescription(description));
			ContentNodeRESTUtils.assertResponseOK(response);

			return response.getGroup();
		});

		Trx.operate(() -> {
			resource.add(String.valueOf(NODE_GROUP), new Group().setName(name).setDescription(description));
		});
	}

	/**
	 * Test creating group without name
	 * @throws NodeException
	 */
	@Test
	@Expected(ex = MissingFieldException.class, message = "Das Feld 'Name' darf nicht leer sein.")
	public void testCreateEmptyName() throws NodeException {
		Trx.operate(() -> {
			new GroupResourceImpl().add(String.valueOf(NODE_GROUP), new Group());
		});
	}

	/**
	 * Test updating a group
	 * @throws NodeException
	 */
	@Test
	public void testUpdate() throws NodeException {
		String originalName = "Original Group";
		String originalDescription = "Original description";
		Group created = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(NODE_GROUP), new Group().setName(originalName).setDescription(originalDescription)).getGroup();
		});

		String newName = "New Group Name";
		String newDescription = "New Group description";
		Group updatedGroup = Trx.supply(() -> {
			return new GroupResourceImpl().update(String.valueOf(created.getId()), new Group().setName(newName).setDescription(newDescription)).getGroup();
		});

		assertThat(updatedGroup).as("Updated group")
			.has(attribute("name", newName))
			.has(attribute("description", newDescription));
	}

	/**
	 * Test deleting a group
	 * @throws NodeException
	 */
	@Test
	public void testDelete() throws NodeException {
		String name = "Group to Delete";
		Group created = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(NODE_GROUP), new Group().setName(name)).getGroup();
		});

		List<Group> groups = Trx.supply(() -> {
			return new GroupResourceImpl().list(null, null, null, null).getItems();
		});
		assertThat(groups).as("Groups after adding").usingFieldByFieldElementComparator().contains(created);

		Trx.operate(() -> {
			new GroupResourceImpl().delete(String.valueOf(created.getId()));
		});

		groups = Trx.supply(() -> {
			return new GroupResourceImpl().list(null, null, null, null).getItems();
		});
		assertThat(groups).as("Groups after deleting").usingFieldByFieldElementComparator().doesNotContain(created);
	}

	/**
	 * Test deleting a group with subgroups
	 * @throws NodeException
	 */
	@Test
	public void testDeleteSubgroups() throws NodeException {
		Group mother = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(NODE_GROUP), new Group().setName("Mother")).getGroup();
		});
		Group child1 = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(mother.getId()), new Group().setName("Child 1")).getGroup();
		});
		Group child2 = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(mother.getId()), new Group().setName("Child 2")).getGroup();
		});
		Group subchild1 = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(child1.getId()), new Group().setName("Subchild 1")).getGroup();
		});
		Group subchild2 = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(child2.getId()), new Group().setName("Subchild 2")).getGroup();
		});

		Trx.operate(() -> {
			assertThat(new GroupResourceImpl().list(null, null, null, null).getItems()).as("Groups after adding").usingFieldByFieldElementComparator()
					.contains(mother, child1, child2, subchild1, subchild2);
		});

		Trx.operate(() -> {
			new GroupResourceImpl().delete(String.valueOf(mother.getId()));
		});

		Trx.operate(() -> {
			assertThat(new GroupResourceImpl().list(null, null, null, null).getItems()).as("Groups after deleting").usingFieldByFieldElementComparator().doesNotContain(mother, child1, child2, subchild1, subchild2);
		});
	}

	/**
	 * Test deleting a group with members (that are also members of other groups)
	 * @throws NodeException
	 */
	@Test
	public void testDeleteWithMembers() throws NodeException {
		// create group
		Group toDelete = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(NODE_GROUP), new Group().setName("Group to Delete")).getGroup();
		});

		// get members of node group
		List<User> members = Trx.supply(() -> {
			return new GroupResourceImpl().users(String.valueOf(NODE_GROUP), null, null, null, null, null).getItems();
		});
		assertThat(members).as("Group members").isNotEmpty();

		// add members to new group
		Trx.operate(() -> {
			for (User member : members) {
				new GroupResourceImpl().addUser(String.valueOf(toDelete.getId()), String.valueOf(member.getId()));
			}
		});

		// check that group has members
		Trx.operate(() -> {
			assertThat(new GroupResourceImpl().users(String.valueOf(toDelete.getId()), null, null, null, null, null).getItems()).as("Members of new group").isNotEmpty();
		});

		// delete
		Trx.operate(() -> {
			new GroupResourceImpl().delete(String.valueOf(toDelete.getId()));
		});

		Trx.operate(() -> {
			assertThat(new GroupResourceImpl().list(null, null, null, null).getItems()).as("Groups after deleting").usingFieldByFieldElementComparator()
					.doesNotContain(toDelete);
		});
	}

	/**
	 * Test deleting a group where a member would lose the last group
	 * @throws NodeException
	 */
	@Test
	@Expected(ex = UserWithoutGroupException.class, message = "Die Gruppe 'Group to Delete' kann nicht gelöscht werden, weil dadurch der Benutzer 'bla' seine letzte Gruppe verlieren würde.")
	public void testDeleteWithLastMember() throws NodeException {
		// create group
		Group toDelete = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(NODE_GROUP), new Group().setName("Group to Delete")).getGroup();
		});

		// create user
		Trx.supply(() -> {
			return new GroupResourceImpl().createUser(String.valueOf(toDelete.getId()), new User().setLogin("bla")).getUser();
		});

		// delete
		Trx.operate(() -> {
			new GroupResourceImpl().delete(String.valueOf(toDelete.getId()));
		});
	}

	/**
	 * Test deleting a group with subgroups where a member would lose the last group
	 * @throws NodeException
	 */
	@Test
	@Expected(ex = UserWithoutGroupException.class, message = "Die Gruppe 'Group to Delete' kann nicht gelöscht werden, weil dadurch der Benutzer 'bla' seine letzte Gruppe verlieren würde.")
	public void testDeleteSubgroupsWithLastMember() throws NodeException {
		// create group
		Group toDelete = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(NODE_GROUP), new Group().setName("Group to Delete")).getGroup();
		});
		// create subgroup
		Group subgroup = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(toDelete.getId()), new Group().setName("Subgroup")).getGroup();
		});

		// create user
		User user = Trx.supply(() -> {
			return new GroupResourceImpl().createUser(String.valueOf(toDelete.getId()), new User().setLogin("bla")).getUser();
		});
		// add user to subgroup
		Trx.operate(() -> {
			new GroupResourceImpl().addUser(String.valueOf(subgroup.getId()), String.valueOf(user.getId()));
		});

		// delete
		Trx.operate(() -> {
			new GroupResourceImpl().delete(String.valueOf(toDelete.getId()));
		});
	}

	/**
	 * Test adding a user
	 * @throws NodeException
	 */
	@Test
	public void testAddUser() throws NodeException {
		Group group = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(NODE_GROUP), new Group().setName("Group")).getGroup();
		});

		// get members of node group
		List<User> members = Trx.supply(() -> {
			return new GroupResourceImpl().users(String.valueOf(NODE_GROUP), null, null, null, null, null).getItems();
		});
		assertThat(members).as("Group members").isNotEmpty();

		// add members to new group
		Trx.operate(() -> {
			for (User member : members) {
				new GroupResourceImpl().addUser(String.valueOf(group.getId()), String.valueOf(member.getId()));
			}
		});

		Trx.operate(() -> {
			assertThat(new GroupResourceImpl().users(String.valueOf(group.getId()), null, null, null, null, null).getItems()).as("Members of new group")
					.usingFieldByFieldElementComparator().containsOnlyElementsOf(members);
		});
	}

	/**
	 * Test adding a user using the user resource
	 * @throws NodeException
	 */
	@Test
	public void testAddUser2() throws NodeException {
		Group group = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(NODE_GROUP), new Group().setName("Group")).getGroup();
		});

		// get members of node group
		List<User> members = Trx.supply(() -> {
			return new GroupResourceImpl().users(String.valueOf(NODE_GROUP), null, null, null, null, null).getItems();
		});
		assertThat(members).as("Group members").isNotEmpty();

		// add members to new group
		Trx.operate(() -> {
			for (User member : members) {
				new UserResourceImpl().addToGroup(String.valueOf(member.getId()), String.valueOf(group.getId()));
			}
		});

		Trx.operate(() -> {
			assertThat(new GroupResourceImpl().users(String.valueOf(group.getId()), null, null, null, null, null).getItems()).as("Members of new group")
					.usingFieldByFieldElementComparator().containsOnlyElementsOf(members);
		});
	}

	/**
	 * Test adding a user twice
	 * @throws NodeException
	 */
	@Test
	public void testAddUserTwice() throws NodeException {
		Group group = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(NODE_GROUP), new Group().setName("Group")).getGroup();
		});

		// get members of node group
		List<User> members = Trx.supply(() -> {
			return new GroupResourceImpl().users(String.valueOf(NODE_GROUP), null, null, null, null, null).getItems();
		});
		assertThat(members).as("Group members").isNotEmpty();
		User user = members.get(0);

		// add user to group twice
		Trx.operate(() -> {
			new GroupResourceImpl().addUser(String.valueOf(group.getId()), String.valueOf(user.getId()));
		});
		Trx.operate(() -> {
			new GroupResourceImpl().addUser(String.valueOf(group.getId()), String.valueOf(user.getId()));
		});

		Trx.operate(() -> {
			assertThat(new GroupResourceImpl().users(String.valueOf(group.getId()), null, null, null, null, null).getItems()).as("Members of new group")
					.usingFieldByFieldElementComparator().hasSize(1).containsOnly(user);
		});
	}

	/**
	 * Test removing a user
	 * @throws NodeException
	 */
	@Test
	public void testRemoveUser() throws NodeException {
		Group group = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(NODE_GROUP), new Group().setName("Group")).getGroup();
		});

		// get members of node group
		List<User> members = Trx.supply(() -> {
			return new GroupResourceImpl().users(String.valueOf(NODE_GROUP), null, null, null, null, null).getItems();
		});
		assertThat(members).as("Group members").isNotEmpty();
		User user = members.get(0);

		// add user to group
		Trx.operate(() -> {
			new GroupResourceImpl().addUser(String.valueOf(group.getId()), String.valueOf(user.getId()));
		});

		Trx.operate(() -> {
			assertThat(new GroupResourceImpl().users(String.valueOf(group.getId()), null, null, null, null, null).getItems()).as("Group members before removing")
					.usingFieldByFieldElementComparator().hasSize(1).containsOnly(user);
		});

		// remove user from group
		Trx.operate(() -> {
			new GroupResourceImpl().removeUser(String.valueOf(group.getId()), String.valueOf(user.getId()));
		});

		Trx.operate(() -> {
			assertThat(new GroupResourceImpl().users(String.valueOf(group.getId()), null, null, null, null, null).getItems()).as("Group members after removing").isEmpty();
		});
	}

	/**
	 * Test removing a user using the user resource
	 * @throws NodeException
	 */
	@Test
	public void testRemoveUser2() throws NodeException {
		Group group = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(NODE_GROUP), new Group().setName("Group")).getGroup();
		});

		// get members of node group
		List<User> members = Trx.supply(() -> {
			return new GroupResourceImpl().users(String.valueOf(NODE_GROUP), null, null, null, null, null).getItems();
		});
		assertThat(members).as("Group members").isNotEmpty();
		User user = members.get(0);

		// add user to group
		Trx.operate(() -> {
			new GroupResourceImpl().addUser(String.valueOf(group.getId()), String.valueOf(user.getId()));
		});

		Trx.operate(() -> {
			assertThat(new GroupResourceImpl().users(String.valueOf(group.getId()), null, null, null, null, null).getItems()).as("Group members before removing")
					.usingFieldByFieldElementComparator().hasSize(1).containsOnly(user);
		});

		// remove user from group
		Trx.operate(() -> {
			new UserResourceImpl().removeFromGroup(String.valueOf(user.getId()), String.valueOf(group.getId()));
		});

		Trx.operate(() -> {
			assertThat(new GroupResourceImpl().users(String.valueOf(group.getId()), null, null, null, null, null).getItems()).as("Group members after removing").isEmpty();
		});
	}

	/**
	 * Test removing a user twice
	 * @throws NodeException
	 */
	@Test
	public void testRemoveUserTwice() throws NodeException {
		Group group = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(NODE_GROUP), new Group().setName("Group")).getGroup();
		});

		// get members of node group
		List<User> members = Trx.supply(() -> {
			return new GroupResourceImpl().users(String.valueOf(NODE_GROUP), null, null, null, null, null).getItems();
		});
		assertThat(members).as("Group members").isNotEmpty();
		User user = members.get(0);

		// add user to group
		Trx.operate(() -> {
			new GroupResourceImpl().addUser(String.valueOf(group.getId()), String.valueOf(user.getId()));
		});

		Trx.operate(() -> {
			assertThat(new GroupResourceImpl().users(String.valueOf(group.getId()), null, null, null, null, null).getItems()).as("Group members before removing")
					.usingFieldByFieldElementComparator().hasSize(1).containsOnly(user);
		});

		// remove user from group twice
		Trx.operate(() -> {
			new GroupResourceImpl().removeUser(String.valueOf(group.getId()), String.valueOf(user.getId()));
		});
		Trx.operate(() -> {
			new GroupResourceImpl().removeUser(String.valueOf(group.getId()), String.valueOf(user.getId()));
		});

		Trx.operate(() -> {
			assertThat(new GroupResourceImpl().users(String.valueOf(group.getId()), null, null, null, null, null).getItems()).as("Group members after removing").isEmpty();
		});
	}

	/**
	 * Test deleting a user from its last group
	 * @throws NodeException
	 */
	@Test
	@Expected(ex = UserWithoutGroupException.class, message = "Der Benutzer 'username' kann nicht von der Gruppe 'Group' entfernt werden, weil das die letzte Gruppe des Benutzers ist.")
	public void testRemoveUserLastGroup() throws NodeException {
		String login = "username";
		Group group = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(NODE_GROUP), new Group().setName("Group")).getGroup();
		});
		User createdUser = Trx.supply(() -> {
			return new GroupResourceImpl().createUser(String.valueOf(group.getId()), new User().setLogin(login)).getUser();
		});

		Trx.operate(() -> {
			new GroupResourceImpl().removeUser(String.valueOf(group.getId()), String.valueOf(createdUser.getId()));
		});
	}

	/**
	 * Test creating a user
	 * @throws NodeException
	 */
	@Test
	public void testCreateUser() throws NodeException {
		String login = "username";
		Group group = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(NODE_GROUP), new Group().setName("Group")).getGroup();
		});

		User createdUser = Trx.supply(() -> {
			return new GroupResourceImpl().createUser(String.valueOf(group.getId()), new User().setLogin(login)).getUser();
		});

		assertThat(createdUser).as("Created user").isNotNull().has(attribute("login", login));
	}

	/**
	 * Test creating a user without login
	 * @throws NodeException
	 */
	@Test
	@Expected(ex = MissingFieldException.class, message = "Das Feld 'Login' darf nicht leer sein.")
	public void testCreateUserNoLogin() throws NodeException {
		Group group = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(NODE_GROUP), new Group().setName("Group")).getGroup();
		});

		Trx.operate(() -> {
			new GroupResourceImpl().createUser(String.valueOf(group.getId()), new User());
		});
	}

	/**
	 * Test creating a user with duplicate login
	 * @throws NodeException
	 */
	@Test
	@Expected(ex = DuplicateValueException.class, message = "Das Feld 'Login' darf nicht den Wert 'node' haben, weil dieser Wert bereits verwendet wird.")
	public void testCreateUserDuplicateLogin() throws NodeException {
		Group group = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(NODE_GROUP), new Group().setName("Group")).getGroup();
		});

		Trx.operate(() -> {
			new GroupResourceImpl().createUser(String.valueOf(group.getId()), new User().setLogin("node"));
		});
	}

	/**
	 * Test creating a user and log in with it's credentials
	 * @throws NodeException
	 */
	@Test
	public void testCreateUserWithPassword() throws NodeException {
		String login = "username";
		String password = "password";
		Group group = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(NODE_GROUP), new Group().setName("Group")).getGroup();
		});

		User createdUser = Trx.supply(() -> {
			return new GroupResourceImpl().createUser(String.valueOf(group.getId()), new User().setLogin(login).setPassword(password)).getUser();
		});
		Trx.operate(() -> {
			assertThat(ContentNodeRESTUtils.login(createdUser.getLogin(), password)).as("SID").isNotNull();
		});
	}

	/**
	 * Test moving a group
	 * @throws NodeException
	 */
	@Test
	public void testMove() throws NodeException {
		// create source and target groups
		Group source = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(NODE_GROUP), new Group().setName("Source Group")).getGroup();
		});
		Group target = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(NODE_GROUP), new Group().setName("Target Group")).getGroup();
		});

		// create group to move
		Group toMove = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(source.getId()), new Group().setName("Moved Group")).getGroup();
		});

		Trx.operate(() -> {
			assertThat(new GroupResourceImpl().subgroups(String.valueOf(source.getId()), null, null, null, null).getItems()).as("Source group children before moving")
					.usingFieldByFieldElementComparator().containsOnly(toMove);
			assertThat(new GroupResourceImpl().subgroups(String.valueOf(target.getId()), null, null, null, null).getItems()).as("Target group children before moving")
					.isEmpty();
		});

		// move
		Trx.operate(() -> {
			new GroupResourceImpl().move(String.valueOf(target.getId()), String.valueOf(toMove.getId()));
		});

		Trx.operate(() -> {
			assertThat(new GroupResourceImpl().subgroups(String.valueOf(source.getId()), null, null, null, null).getItems()).as("Source group children after moving")
					.isEmpty();
			assertThat(new GroupResourceImpl().subgroups(String.valueOf(target.getId()), null, null, null, null).getItems()).as("Target group children after moving")
					.usingFieldByFieldElementComparator().containsOnly(toMove);
		});
	}

	/**
	 * Test moving a group into a subgroup
	 * @throws NodeException
	 */
	@Test
	@Expected(ex = MovingGroupNotPossibleException.class, message = "Die Gruppe 'Moved Group' kann nicht in die Gruppe 'Subgroup' verschoben werden, weil das eine Untergruppe ist.")
	public void testMoveIntoSubgroup() throws NodeException {
		// create group to move
		Group toMove = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(NODE_GROUP), new Group().setName("Moved Group")).getGroup();
		});
		// create subgroup
		Group subgroup = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(toMove.getId()), new Group().setName("Subgroup")).getGroup();
		});

		// move
		Trx.operate(() -> {
			new GroupResourceImpl().move(String.valueOf(subgroup.getId()), String.valueOf(toMove.getId()));
		});
	}

	/**
	 * Test moving a group into target, when target already contains a group with the name
	 * @throws NodeException
	 */
	@Test
	@Expected(ex = MovingGroupNotPossibleException.class, message = "Die Gruppe 'Moved Group' kann nicht in die Gruppe 'Target Group' verschoben werden, weil es dort schon eine Gruppe mit diesem Namen gibt.")
	public void testMoveDuplicateName() throws NodeException {
		// create source and target groups
		Group source = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(NODE_GROUP), new Group().setName("Source Group")).getGroup();
		});
		Group target = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(NODE_GROUP), new Group().setName("Target Group")).getGroup();
		});

		// create group to move
		Group toMove = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(source.getId()), new Group().setName("Moved Group")).getGroup();
		});
		// create conflicting group
		Trx.operate(() -> {
			new GroupResourceImpl().add(String.valueOf(target.getId()), new Group().setName("Moved Group"));
		});

		// move
		Trx.operate(() -> {
			new GroupResourceImpl().move(String.valueOf(target.getId()), String.valueOf(toMove.getId()));
		});
	}
}
