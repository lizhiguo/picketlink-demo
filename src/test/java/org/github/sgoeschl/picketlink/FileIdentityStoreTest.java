package org.github.sgoeschl.picketlink;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.PermissionManager;
import org.picketlink.idm.config.IdentityConfigurationBuilder;
import org.picketlink.idm.internal.DefaultPartitionManager;
import org.picketlink.idm.model.Attribute;
import org.picketlink.idm.model.IdentityType;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.Group;
import org.picketlink.idm.model.basic.Realm;
import org.picketlink.idm.model.basic.Role;
import org.picketlink.idm.permission.IdentityPermission;
import org.picketlink.idm.permission.Permission;
import org.picketlink.idm.query.IdentityQuery;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import static org.junit.Assert.*;

/**
 * This test using the FileIdentityStore covering the following parts
 * <ul>
 *   <li>Realms</li>
 *   <li>Groups</li>
 *   <li>Roles</li>
 *   <li>Permissions</li>
 * </ul>
 */
public class FileIdentityStoreTest {

    private File fileStoreDirectory = new File("./target/store");

    public static final String REALM_NAME = "AMS";

    @Before
    public void setUp() throws Exception {
        FileUtils.deleteDirectory(fileStoreDirectory);
        PartitionManager partitionManager = createPartitionManager(fileStoreDirectory);
        createTestRealm(partitionManager);
    }

    @Test
    public void testUsePersistentModel() throws Exception {

        // bootstrap to get the all important "partitionManager"
        PartitionManager partitionManager = createPartitionManager(fileStoreDirectory);
        
        // get the AMS realm
        
        Realm realm = partitionManager.getPartition(Realm.class, REALM_NAME);
        assertNotNull(realm);
        assertEquals(1, realm.getAttributes().size());
        assertEquals("realmAttributeValue", realm.getAttribute("realmAttributeName").getValue().toString());

        // create an IdentityManager to work with the basic model
        
        IdentityManager identityManager = partitionManager.createIdentityManager(realm);

        // get the various groups

        Group groupBerlinZvb = BasicModel.getGroup(identityManager, "/Berlin/ZVB");
        assertNotNull(groupBerlinZvb);
        assertEquals(1, groupBerlinZvb.getAttributes().size());
        assertEquals("groupBerlinZvbAttributeValue", groupBerlinZvb.getAttribute("groupBerlinZvbAttributeName").getValue().toString());

        Group groupBerlinMitte = BasicModel.getGroup(identityManager, "/Berlin/Mitte");
        assertNotNull(groupBerlinMitte);
        assertEquals(1, groupBerlinMitte.getAttributes().size());
        assertEquals("groupBerlinMitteAttributeValue", groupBerlinMitte.getAttribute("groupBerlinMitteAttributeName").getValue().toString());

        Group groupBerlinPankow = BasicModel.getGroup(identityManager, "/Berlin/Pankow");
        assertNotNull(groupBerlinPankow);
        assertEquals(1, groupBerlinPankow.getAttributes().size());
        assertEquals("groupBerlinPankowAttributeValue", groupBerlinPankow.getAttribute("groupBerlinPankowAttributeName").getValue().toString());

        // get the role "Sachbearbeiter"
        
        Role roleSachbearbeiter = BasicModel.getRole(identityManager, "Sachbearbeiter");
        assertNotNull(roleSachbearbeiter);
        assertEquals(1, roleSachbearbeiter.getAttributes().size());
        assertEquals("roleSachbearbeiterAttributeValue", roleSachbearbeiter.getAttribute("roleSachbearbeiterAttributeName").getValue().toString());

        Role roleBeteiligter = BasicModel.getRole(identityManager, "Beteiligter");
        assertNotNull(roleBeteiligter);
        assertEquals(1, roleBeteiligter.getAttributes().size());
        assertEquals("roleBeteiligterAttributeValue", roleBeteiligter.getAttribute("roleBeteiligterAttributeName").getValue().toString());

        // tinker with permissions
        // please note that this in 2.6.1.Final not supported
        // but only with 2.7.0.CR1 and later

        PermissionManager permissionManager = partitionManager.createPermissionManager(realm);

        assertEquals(5, permissionManager.listPermissions("Stammdaten").size());
        assertEquals(2, permissionManager.listPermissions("Meldung").size());
        assertEquals(4, permissionManager.listPermissions("Anliegen").size());

        assertFalse(hasPermission(roleSachbearbeiter, permissionManager.listPermissions("Meldung", "create")));
        assertTrue(hasPermission(roleSachbearbeiter, permissionManager.listPermissions("Meldung", "read")));
        assertTrue(hasPermission(roleSachbearbeiter, permissionManager.listPermissions("Meldung", "update")));
        assertFalse(hasPermission(roleSachbearbeiter, permissionManager.listPermissions("Meldung", "delete")));
        
        assertTrue(hasPermission(roleSachbearbeiter, permissionManager.listPermissions("Anliegen", "create")));
        assertTrue(hasPermission(roleSachbearbeiter, permissionManager.listPermissions("Anliegen", "read")));
        assertTrue(hasPermission(roleSachbearbeiter, permissionManager.listPermissions("Anliegen", "update")));
        assertFalse(hasPermission(roleSachbearbeiter, permissionManager.listPermissions("Anliegen", "delete")));

        assertFalse(hasPermission(roleBeteiligter, permissionManager.listPermissions("Meldung", "create")));
        assertFalse(hasPermission(roleBeteiligter, permissionManager.listPermissions("Meldung", "read")));
        assertFalse(hasPermission(roleBeteiligter, permissionManager.listPermissions("Meldung", "update")));
        assertFalse(hasPermission(roleBeteiligter, permissionManager.listPermissions("Meldung", "delete")));

        assertFalse(hasPermission(roleBeteiligter, permissionManager.listPermissions("Anliegen", "create")));
        assertTrue(hasPermission(roleBeteiligter, permissionManager.listPermissions("Anliegen", "read")));
        assertFalse(hasPermission(roleBeteiligter, permissionManager.listPermissions("Anliegen", "update")));
        assertFalse(hasPermission(roleBeteiligter, permissionManager.listPermissions("Anliegen", "delete")));

        // dump the whole thing
        
        System.out.println(toString(partitionManager, realm));
        
        return;
    }
    
    private String toString(PartitionManager partitionManager, Realm realm) {

        StringBuffer result = new StringBuffer();
        IdentityManager identityManager = partitionManager.createIdentityManager(realm);

        // realm
        
        result.append("=== REALM ===").append("\n");
        result.append("Realm: name=" + realm.getName()).append("\n");
        result.append("\n");
        
        // groups
        
        IdentityQuery groupIdentityQuery = identityManager.createIdentityQuery(Group.class);
        List<Group> groupList = groupIdentityQuery.getResultList();
        result.append("=== GROUPS ===").append("\n");
        for(Group group : groupList) {
            result.append("Group: name=" + group.getName() + ", path=" + group.getPath()).append("\n");
        }
        result.append("\n");

        // roles

        IdentityQuery roleIdentityQuery = identityManager.createIdentityQuery(Role.class);
        List<Role> roleList = roleIdentityQuery.getResultList();
        result.append("=== ROLES ===").append("\n");
        for(Role role : roleList) {
            result.append("Role: name=" + role.getName()).append("\n");
        }
        result.append("\n");

        return result.toString();
    }

    private void createTestRealm(PartitionManager partitionManager) {

        // create the realm which works as a starting point
        
        Realm realm = new Realm(REALM_NAME);
        realm.setAttribute(new Attribute<Serializable>("realmAttributeName", "realmAttributeValue"));
        partitionManager.add(realm);

        PermissionManager permissionManager = partitionManager.createPermissionManager(realm);
        
        // create and store 4 groups
        
        Group groupBerlin = new Group("Berlin");
        partitionManager.createIdentityManager(realm).add(groupBerlin);

        Group groupBerlinZvb = new Group("ZVB", groupBerlin);
        groupBerlinZvb.setAttribute(new Attribute<Serializable>("groupBerlinZvbAttributeName", "groupBerlinZvbAttributeValue"));
        partitionManager.createIdentityManager(realm).add(groupBerlinZvb);

        Group groupBerlinMitte = new Group("Mitte", groupBerlin);
        groupBerlinMitte.setAttribute(new Attribute<Serializable>("groupBerlinMitteAttributeName", "groupBerlinMitteAttributeValue"));
        partitionManager.createIdentityManager(realm).add(groupBerlinMitte);
        
        Group groupBerlinPankow = new Group("Pankow", groupBerlin);
        groupBerlinPankow.setAttribute(new Attribute<Serializable>("groupBerlinPankowAttributeName", "groupBerlinPankowAttributeValue"));
        partitionManager.createIdentityManager(realm).add(groupBerlinPankow);

        // create two roles

        Role roleSachbearbeiter = new Role("Sachbearbeiter");
        roleSachbearbeiter.setAttribute(new Attribute<Serializable>("roleSachbearbeiterAttributeName", "roleSachbearbeiterAttributeValue"));
        partitionManager.createIdentityManager(realm).add(roleSachbearbeiter);

        Role roleBeteiligter = new Role("Beteiligter");
        roleBeteiligter.setAttribute(new Attribute<Serializable>("roleBeteiligterAttributeName", "roleBeteiligterAttributeValue"));
        partitionManager.createIdentityManager(realm).add(roleBeteiligter);

        // create permissions for (roles/entities) && (group/permissions)

        permissionManager.grantPermission(groupBerlin, "Stammdaten", "read");

        permissionManager.grantPermission(groupBerlinZvb, "Stammdaten", "create,read,update,delete");

        permissionManager.grantPermission(roleSachbearbeiter, "Meldung", "read,update");
        permissionManager.grantPermission(roleSachbearbeiter, "Anliegen", "create,read,update");

        permissionManager.grantPermission(roleBeteiligter, "Anliegen", "read");

    }
    
    private PartitionManager createPartitionManager(File directory) {
        IdentityConfigurationBuilder builder = createIdentityConfigurationBuilder(directory);
        return new DefaultPartitionManager(builder.build());
    }
    
    private IdentityConfigurationBuilder createIdentityConfigurationBuilder(File directory) {

        IdentityConfigurationBuilder builder = new IdentityConfigurationBuilder();

        builder
                .named("file-store-preserve-state")
                .stores()
                .file()
                .preserveState(true)
                .workingDirectory(directory.getAbsolutePath())
                .supportAllFeatures();

        return builder;
    }
    
    public boolean hasPermission(IdentityType identityType, List<Permission> permissions) {
        for (Permission permission : permissions) {
            if (IdentityPermission.class.isInstance(permission)) {
                IdentityPermission identityPermission = (IdentityPermission) permission;

                if (identityPermission.getAssignee().equals(identityType)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     private List<IdentityConfiguration> buildFromFile(String configFilePath) {
     ClassLoader tcl = Thread.currentThread().getContextClassLoader();
     InputStream configStream = tcl.getResourceAsStream(configFilePath);
     XMLConfigurationProvider xmlConfigurationProvider = new XMLConfigurationProvider();
     IdentityConfigurationBuilder idmConfigBuilder = xmlConfigurationProvider.readIDMConfiguration(configStream);
     assertNotNull(idmConfigBuilder);
     return idmConfigBuilder.buildAll();
     }
     */
}