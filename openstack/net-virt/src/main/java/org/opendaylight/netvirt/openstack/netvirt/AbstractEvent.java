/*
 * Copyright (c) 2014, 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netvirt.openstack.netvirt;

import org.opendaylight.netvirt.openstack.netvirt.api.Action;

/**
 * Abstract class for events used by neutron northbound and southbound events.
 */
public abstract class AbstractEvent {

    public enum HandlerType {
        SOUTHBOUND,
        NEUTRON_FLOATING_IP,
        NEUTRON_NETWORK,
        NEUTRON_PORT,
        NEUTRON_PORT_SECURITY,
        NEUTRON_ROUTER,
        NEUTRON_SUBNET,
        NEUTRON_FWAAS,
        NEUTRON_LOAD_BALANCER,
        NEUTRON_LOAD_BALANCER_POOL,
        NEUTRON_LOAD_BALANCER_POOL_MEMBER,
        NODE,
        NEUTRON_L3_ADAPTER,
        DISTRIBUTED_ARP_SERVICE;

        public static final int size = HandlerType.values().length;
    }

    private HandlerType handlerType;
    private Action action;
    private int transactionId;

    public int getTransactionId() {
        return transactionId;
    }

    private static int txId = 0;
    private static int incTxId() {
        return ++txId;
    }
    public static int getTxId() {
        return txId;
    }

    private AbstractEvent() {
        // this is private to force proper construction
    }

    protected AbstractEvent(HandlerType handlerType, Action action) {
        this.handlerType = handlerType;
        this.action = action;
        this.transactionId = incTxId();
    }

    public HandlerType getHandlerType() {
        return handlerType;
    }

    public Action getAction() {
        return action;
    }

    @Override
    public String toString() {
        return "AbstractEvent [transactionId=" + transactionId
                + " handlerType=" + handlerType + " action=" + action + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((handlerType == null) ? 0 : handlerType.hashCode());
        result = prime * result + ((action == null) ? 0 : action.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AbstractEvent other = (AbstractEvent) obj;
        if (handlerType == null) {
            if (other.handlerType != null) {
                return false;
            }
        } else if (!handlerType.equals(other.handlerType)) {
            return false;
        }
        if (action == null) {
            if (other.action != null) {
                return false;
            }
        } else if (!action.equals(other.action)) {
            return false;
        }
        return true;
    }
}
