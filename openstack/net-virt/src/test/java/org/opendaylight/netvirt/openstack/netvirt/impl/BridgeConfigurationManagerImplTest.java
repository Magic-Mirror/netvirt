/*
* Copyright (c) 2014, 2016 Intel Corp. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.netvirt.openstack.netvirt.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.opendaylight.netvirt.openstack.netvirt.api.OvsdbTables;
import org.opendaylight.netvirt.openstack.netvirt.api.ConfigurationService;
import org.opendaylight.netvirt.openstack.netvirt.api.NetworkingProviderManager;
import org.opendaylight.netvirt.openstack.netvirt.api.Southbound;
import org.opendaylight.netvirt.openstack.netvirt.translator.NeutronNetwork;
import org.opendaylight.netvirt.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeSystem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105
        .OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology
        .Node;
import org.osgi.framework.ServiceReference;

/**
 * Test class for BridgeConfigurationManagerImpl
 *
 * @author Marcus Koontz
 * @author Alexis de Talhouet
 * @author Sam Hague (shague@redhat.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class BridgeConfigurationManagerImplTest {
    @Mock private Node node;
    @Mock private OvsdbBridgeAugmentation bridge;
    @Mock private OvsdbTerminationPointAugmentation port;
    @Mock private NeutronNetwork neutronNetwork;
    @Mock private ConfigurationService configurationService;
    @Mock private Southbound southbound;
    @InjectMocks public static BridgeConfigurationManagerImpl bridgeConfigurationManagerImpl;

    private static final String ADDRESS = "127.0.0.1";
    private static final String BR_INT = "br-int";
    private static final String ETH1 = "eth1";
    private static final String ETH2 = "eth2";
    private static final String ETH3 = "eth3";
    private static final String PORT_BR_INT = "br-int";
    private static final String BRIDGE_UUID = "f527b951-3934-4182-9f29-33fc09f6f0c6";
    private static final String PHYSNET1 = "physnet1";
    private static final String PHYSNET2 = "physnet2";
    private static final String PHYSNET3 = "physnet3";
    private static final String PROVIDER_MAPPINGS = PHYSNET1 + ":" + ETH1 + "," + PHYSNET2 + ":" + ETH2;
    private static final String PROVIDER_MAPPINGS_DEFAULT = PHYSNET1 + ":" + ETH1;

    @Test
    public void testGetBridgeUuid() {
        when(southbound.getBridgeUuid(any(Node.class), anyString()))
                .thenReturn(null)
                .thenReturn(BRIDGE_UUID);

        assertEquals("Error, null should have been returned", null,
                bridgeConfigurationManagerImpl.getBridgeUuid(node, BR_INT));
        assertEquals("Error, did not return UUID of correct bridge", BRIDGE_UUID,
                bridgeConfigurationManagerImpl.getBridgeUuid(node, BR_INT));
        verify(southbound, times(2)).getBridgeUuid(any(Node.class), anyString());
    }

    @Test
    public void testIsNodeNeutronReady() throws Exception {
        when(southbound.getBridge(any(Node.class), anyString()))
                .thenReturn(null)
                .thenReturn(bridge);

        verifyNoMoreInteractions(configurationService);
        assertFalse("Error, did not return correct boolean from isNodeNeutronReady",
                bridgeConfigurationManagerImpl.isNodeNeutronReady(node));

        when(configurationService.getIntegrationBridgeName()).thenReturn(BR_INT);
        assertTrue("Error, did not return correct boolean from isNodeNeutronReady",
                bridgeConfigurationManagerImpl.isNodeNeutronReady(node));

        verify(configurationService, times(2)).getIntegrationBridgeName();
        verify(southbound, times(2)).getBridge(any(Node.class), anyString());
    }

    @Test
    public void testIsNodeOverlayReady() throws Exception {
        when(southbound.getBridge(any(Node.class), anyString()))
                .thenReturn(null)
                .thenReturn(bridge);

        BridgeConfigurationManagerImpl bridgeConfigurationManagerImplSpy =
                spy(new BridgeConfigurationManagerImpl());
        doReturn(false).when(bridgeConfigurationManagerImplSpy).isNodeNeutronReady(any(Node.class));
        bridgeConfigurationManagerImplSpy.setConfigurationService(configurationService);
        bridgeConfigurationManagerImplSpy.setSouthbound(southbound);

        verifyNoMoreInteractions(configurationService);

        assertFalse("Error, did not return correct boolean from isNodeOverlayReady",
                bridgeConfigurationManagerImplSpy.isNodeOverlayReady(node));

        doReturn(true).when(bridgeConfigurationManagerImplSpy).isNodeNeutronReady(any(Node.class));

        assertFalse("Error, did not return correct boolean from isNodeOverlayReady",
                bridgeConfigurationManagerImplSpy.isNodeOverlayReady(node));

        assertTrue("Error, did not return correct boolean from isNodeOverlayReady",
                bridgeConfigurationManagerImplSpy.isNodeOverlayReady(node));

        verify(configurationService, times(2)).getNetworkBridgeName();
        verify(southbound, times(2)).getBridge(any(Node.class), anyString());
    }

    @Test
    public void testIsPortOnBridge() throws Exception {
        when(southbound.extractTerminationPointAugmentation(any(Node.class), anyString()))
                .thenReturn(null)
                .thenReturn(port);

        assertFalse("Error, port " + PORT_BR_INT + " should not be found",
                bridgeConfigurationManagerImpl.isPortOnBridge(node, PORT_BR_INT));
        assertTrue("Error, port " + PORT_BR_INT + " should be found",
                bridgeConfigurationManagerImpl.isPortOnBridge(node, PORT_BR_INT));
        verify(southbound, times(2)).extractTerminationPointAugmentation(any(Node.class), anyString());
    }

    @Test
    public void testIsNodeTunnelReady() throws Exception {
        when(southbound.isBridgeOnOvsdbNode(any(Node.class), anyString()))
                .thenReturn(false)
                .thenReturn(true);

        verifyNoMoreInteractions(configurationService);
        assertFalse("Error, did not return correct boolean from isNodeTunnelReady",
                bridgeConfigurationManagerImpl.isNodeTunnelReady(node, node));

        when(configurationService.isL3ForwardingEnabled()).thenReturn(false);
        when(configurationService.getIntegrationBridgeName()).thenReturn(BR_INT);
        assertTrue("Error, did not return correct boolean from isNodeTunnelReady",
                bridgeConfigurationManagerImpl.isNodeTunnelReady(node, node));

        verify(configurationService, times(1)).isL3ForwardingEnabled();
        verify(configurationService, times(3)).getIntegrationBridgeName();
        verify(southbound, times(2)).isBridgeOnOvsdbNode(any(Node.class), anyString());
    }

    @Test
    public void testIsNodeVlanReady() throws Exception {
        when(southbound.isBridgeOnOvsdbNode(any(Node.class), anyString()))
                .thenReturn(false)
                .thenReturn(true);

        when(southbound.extractTerminationPointAugmentation(any(Node.class), anyString()))
                .thenReturn(null)
                .thenReturn(port);

        when(neutronNetwork.getProviderPhysicalNetwork()).thenReturn("test");

        verifyNoMoreInteractions(configurationService);
        assertFalse("Error, did not return correct boolean from isNodeTunnelReady",
                bridgeConfigurationManagerImpl.isNodeVlanReady(node, node, neutronNetwork));

        BridgeConfigurationManagerImpl bridgeConfigurationManagerImplSpy =
                spy(new BridgeConfigurationManagerImpl());
        doReturn(ETH1).when(bridgeConfigurationManagerImplSpy).getPhysicalInterfaceName(any(Node.class), anyString());
        bridgeConfigurationManagerImplSpy.setConfigurationService(configurationService);
        bridgeConfigurationManagerImplSpy.setSouthbound(southbound);

        assertFalse("Error, did not return correct boolean from isNodeVlanReady",
                bridgeConfigurationManagerImpl.isNodeVlanReady(node, node, neutronNetwork));

        assertTrue("Error, did not return correct boolean from isNodeVlanReady",
                bridgeConfigurationManagerImpl.isNodeVlanReady(node, node, neutronNetwork));

        verify(configurationService, times(3)).getIntegrationBridgeName();
        verify(neutronNetwork, times(2)).getProviderPhysicalNetwork();
    }

    @Test
    public void testIsNodeL3Ready() {
        when(southbound.isBridgeOnOvsdbNode(any(Node.class), anyString())).thenReturn(true);
        when(southbound.extractTerminationPointAugmentation(any(Node.class), anyString())).thenReturn(mock(OvsdbTerminationPointAugmentation.class));
        when(southbound.readBridgeNode(any(Node.class), anyString())).thenReturn(mock(Node.class));

        assertTrue("Error, isNodeL3Ready didn't return true", bridgeConfigurationManagerImpl.isNodeL3Ready(node, node));
    }

    @Test
    public void testPrepareNode() {
        when(configurationService.getIntegrationBridgeName()).thenReturn(BR_INT);
        when(southbound.isBridgeOnOvsdbNode(any(Node.class), anyString())).thenReturn(false);

        // TODO Figure out the strings
        //when(ConfigProperties.getProperty(any(Class.class), anyString())).thenReturn(ADDRESS);

        when(southbound.addBridge(any(Node.class), anyString(), anyList(), eq(DatapathTypeSystem.class))).thenReturn(true);
        when(configurationService.isL3ForwardingEnabled()).thenReturn(true);

        bridgeConfigurationManagerImpl.prepareNode(node);

        // TODO This test doesn't verify anything
    }

    @Test
    public void testCreateLocalNetwork() throws Exception {
        NeutronNetwork neutronNetworkMock = mock(NeutronNetwork.class, RETURNS_MOCKS);
        String networkTypes[] = {"vlan", "vxlan", "gre"};
        BridgeConfigurationManagerImpl bridgeConfigurationManagerImplSpy =
                spy(new BridgeConfigurationManagerImpl());
        bridgeConfigurationManagerImplSpy.setConfigurationService(configurationService);
        bridgeConfigurationManagerImplSpy.setSouthbound(southbound);

        for (String networkType : networkTypes) {
            when(neutronNetworkMock.getProviderNetworkType()).thenReturn(networkType);
            when(southbound.readOvsdbNode(any(Node.class))).thenReturn(node);

            doAnswer(new Answer<Boolean>() {
                @Override
                public Boolean answer(InvocationOnMock invocation) {
                    return Boolean.TRUE;
                }
            }).when(bridgeConfigurationManagerImplSpy).isNodeVlanReady(any(Node.class), any(Node.class), any(NeutronNetwork.class));

            doAnswer(new Answer<Boolean>() {
                @Override
                public Boolean answer(InvocationOnMock invocation) {
                    return Boolean.TRUE;
                }
            }).when(bridgeConfigurationManagerImplSpy).isNodeTunnelReady(any(Node.class), any(Node.class));

            assertTrue("bridgeConfigMock.isNodeVlanReady is not true",
                    bridgeConfigurationManagerImplSpy.isNodeVlanReady(node, node, neutronNetworkMock));
            assertTrue("bridgeConfigMock.isNodeTunnelReady is not true",
                    bridgeConfigurationManagerImplSpy.isNodeTunnelReady(node, node));

            assertTrue("Error, isCreated is not true for " + networkType,
                    bridgeConfigurationManagerImplSpy.createLocalNetwork(node, neutronNetworkMock));
            switch (networkType) {
                case "vlan":
                    verify(neutronNetworkMock, times(1)).getProviderNetworkType();
                    break;
                case "vxlan":
                    verify(neutronNetworkMock, times(2)).getProviderNetworkType();
                    break;
                case "gre":
                    verify(neutronNetworkMock, times(3)).getProviderNetworkType();
                    break;
            }
            reset(neutronNetworkMock);
            reset(node);
            reset(bridgeConfigurationManagerImplSpy);
        }
    }

    @Test
    public void testGetPhysicalInterfaceName() throws Exception {
        when(southbound.getOtherConfig(any(Node.class), Matchers.eq(OvsdbTables.OPENVSWITCH), anyString()))
                .thenReturn(null)
                .thenReturn(null)
                .thenReturn(PROVIDER_MAPPINGS);
        String networkNames[] = {PHYSNET1, PHYSNET2};
        String interfaceNames[] = {ETH1, ETH2};

        verifyNoMoreInteractions(configurationService);
        when(configurationService.getDefaultProviderMapping()).thenReturn(PROVIDER_MAPPINGS_DEFAULT);

        assertNull("Error, should not have found " + PHYSNET2 + ":" + ETH2,
                bridgeConfigurationManagerImpl.getPhysicalInterfaceName(node, PHYSNET2));
        assertEquals("Error, should have found " + PHYSNET1 + ":" + ETH1,
                ETH1, bridgeConfigurationManagerImpl.getPhysicalInterfaceName(node, PHYSNET1));
        for (int i = 0; i < networkNames.length; i++) {
            assertEquals("Error, network: " + networkNames[i]
                            + ", did not match interface: "+ interfaceNames[i],
                    interfaceNames[i],
                    bridgeConfigurationManagerImpl.getPhysicalInterfaceName(node, networkNames[i]));
        }
        assertNull(PHYSNET1, bridgeConfigurationManagerImpl.getPhysicalInterfaceName(node, PHYSNET3));
        verify(configurationService, times(5)).getProviderMappingsKey();
        verify(configurationService, times(2)).getDefaultProviderMapping();
        verify(southbound, times(5)).getOtherConfig(any(Node.class), eq(OvsdbTables.OPENVSWITCH), anyString());
    }

    @Test
    public void testGetAllPhysicalInterfaceNames() throws Exception {
        when(southbound.getOtherConfig(any(Node.class), eq(OvsdbTables.OPENVSWITCH), anyString()))
                .thenReturn(null)
                .thenReturn(PROVIDER_MAPPINGS);

        verifyNoMoreInteractions(configurationService);
        when(configurationService.getDefaultProviderMapping()).thenReturn(PROVIDER_MAPPINGS_DEFAULT);

        List<String> interfaces = bridgeConfigurationManagerImpl.getAllPhysicalInterfaceNames(node);
        assertEquals("Error, should have found 1 interface", 1, interfaces.size());
        assertTrue("Error, should have found " + ETH1, interfaces.contains(ETH1));
        assertFalse("Error, should not have found " + ETH2, interfaces.contains(ETH2));
        interfaces = bridgeConfigurationManagerImpl.getAllPhysicalInterfaceNames(node);
        assertEquals("Error, should have found 2 interfaces", 2, interfaces.size());
        assertTrue("Error, should have found " + ETH1, interfaces.contains(ETH1));
        assertTrue("Error, should have found " + ETH1, interfaces.contains(ETH2));
        assertFalse("Error, should not have found " + ETH3, interfaces.contains(ETH3));

        verify(configurationService, times(2)).getProviderMappingsKey();
        verify(configurationService, times(1)).getDefaultProviderMapping();
        verify(southbound, times(2)).getOtherConfig(any(Node.class), eq(OvsdbTables.OPENVSWITCH), anyString());
    }

    @Test
    public void testSetDependencies() throws Exception {
        ConfigurationService configurationService = mock(ConfigurationService.class);
        NetworkingProviderManager networkingProviderManager = mock(NetworkingProviderManager.class);
        Southbound southbound = mock(Southbound.class);

        ServiceHelper.overrideGlobalInstance(ConfigurationService.class, configurationService);
        ServiceHelper.overrideGlobalInstance(NetworkingProviderManager.class, networkingProviderManager);
        ServiceHelper.overrideGlobalInstance(Southbound.class, southbound);

        bridgeConfigurationManagerImpl.setDependencies(mock(ServiceReference.class));

        assertEquals("Error, did not return the correct object", getField("configurationService"), configurationService);
        assertEquals("Error, did not return the correct object", getField("networkingProviderManager"), networkingProviderManager);
        assertEquals("Error, did not return the correct object", getField("southbound"), southbound);
    }

    private Object getField(String fieldName) throws Exception {
        Field field = BridgeConfigurationManagerImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(bridgeConfigurationManagerImpl);
    }
}