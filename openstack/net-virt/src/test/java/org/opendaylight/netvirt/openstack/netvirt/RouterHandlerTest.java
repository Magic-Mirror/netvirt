/*
 * Copyright (c) 2015, 2016 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netvirt.openstack.netvirt;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.HttpURLConnection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.netvirt.openstack.netvirt.api.Action;
import org.opendaylight.netvirt.openstack.netvirt.api.EventDispatcher;
import org.opendaylight.netvirt.openstack.netvirt.impl.NeutronL3Adapter;
import org.opendaylight.netvirt.openstack.netvirt.translator.NeutronRouter;
import org.opendaylight.netvirt.openstack.netvirt.translator.NeutronRouter_Interface;
import org.opendaylight.netvirt.utils.servicehelper.ServiceHelper;
import org.osgi.framework.ServiceReference;

/**
 * Unit test fort {@link RouterHandler}
 */
@RunWith(MockitoJUnitRunner.class)
public class RouterHandlerTest {

    @InjectMocks RouterHandler routerHandler;

    @Mock NeutronL3Adapter neutronL3Adapter;

    @Test
    public void testCanCreateRouter() {
        assertEquals("Error, canCreateRouter() did not return the correct HTTP flag", HttpURLConnection.HTTP_OK, routerHandler.canCreateRouter(mock(NeutronRouter.class)));
    }

    @Test
    public void testCanUpdateRouter() {
        assertEquals("Error, canUpdateRouter() did not return the correct HTTP flag", HttpURLConnection.HTTP_OK, routerHandler.canUpdateRouter(mock(NeutronRouter.class), mock(NeutronRouter.class)));
    }

    @Test
    public void testCanDeleteRouter() {
        assertEquals("Error, canDeleteRouter() did not return the correct HTTP flag", HttpURLConnection.HTTP_OK, routerHandler.canDeleteRouter(mock(NeutronRouter.class)));
    }

    @Test
    public void testCanAttachAndDettachInterface() {
        NeutronRouter router = mock(NeutronRouter.class);
        when(router.getName()).thenReturn("router_name");

        NeutronRouter_Interface routerInterface = mock(NeutronRouter_Interface.class);
        when(routerInterface.getPortUUID()).thenReturn("portUUID");
        when(routerInterface.getSubnetUUID()).thenReturn("subnetUUID");

        assertEquals("Error, canAttachInterface() did not return the correct HTTP flag", HttpURLConnection.HTTP_OK, routerHandler.canAttachInterface(router, routerInterface));
        assertEquals("Error, canDetachInterface() did not return the correct HTTP flag", HttpURLConnection.HTTP_OK, routerHandler.canDetachInterface(router, routerInterface));
    }

    @Test
    public void testProcessEvent() {
        NorthboundEvent ev = mock(NorthboundEvent.class);

        when(ev.getAction()).thenReturn(Action.UPDATE);
        when(ev.getRouter()).thenReturn(mock(NeutronRouter.class));
        when(ev.getRouterInterface())
                                .thenReturn(null)
                                .thenReturn(mock(NeutronRouter_Interface.class));

        routerHandler.processEvent(ev);
        verify(neutronL3Adapter, times(1)).handleNeutronRouterEvent(ev.getRouter(), Action.UPDATE);
        routerHandler.processEvent(ev);
        verify(neutronL3Adapter, times(1)).handleNeutronRouterInterfaceEvent(ev.getRouter(), ev.getRouterInterface(), Action.UPDATE);
    }

    @Test
    public void testSetDependencies() throws Exception {
        NeutronL3Adapter neutronL3Adapter = mock(NeutronL3Adapter.class);
        EventDispatcher eventDispatcher = mock(EventDispatcher.class);

        ServiceHelper.overrideGlobalInstance(NeutronL3Adapter.class, neutronL3Adapter);
        ServiceHelper.overrideGlobalInstance(EventDispatcher.class, eventDispatcher);

        routerHandler.setDependencies(mock(ServiceReference.class));

        assertEquals("Error, did not return the correct object", getField("neutronL3Adapter"), neutronL3Adapter);
        assertEquals("Error, did not return the correct object", routerHandler.eventDispatcher, eventDispatcher);
    }

    private Object getField(String fieldName) throws Exception {
        Field field = RouterHandler.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(routerHandler);
    }
}
