/*
 * Copyright 2018 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.linecorp.centraldogma.client.armeria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import com.linecorp.armeria.client.Endpoint;
import com.linecorp.armeria.client.endpoint.EndpointGroup;
import com.linecorp.armeria.client.endpoint.EndpointGroupRegistry;

public class ArmeriaCentralDogmaBuilderTest {
    @Test
    public void buildingWithProfile() {
        final String groupName = "centraldogma-profile-alice";
        try {
            final ArmeriaCentralDogmaBuilder b = new ArmeriaCentralDogmaBuilder();
            b.profile("alice");
            final Endpoint endpoint = b.endpoint();
            assertThat(endpoint.isGroup()).isTrue();
            assertThat(endpoint.groupName()).isEqualTo(groupName);

            final EndpointGroup group = EndpointGroupRegistry.get(groupName);
            assertThat(group).isNotNull();

            final List<Endpoint> endpoints = group.endpoints();
            assertThat(endpoints).isNotNull();
            assertThat(endpoints).containsExactlyInAnyOrder(
                    Endpoint.of("alice.com", 36462),
                    Endpoint.of("bob.com", 8080));
        } finally {
            EndpointGroupRegistry.unregister(groupName);
        }
    }

    @Test
    public void buildingWithSingleHost() {
        final long id = AbstractArmeriaCentralDogmaBuilder.nextAnonymousGroupId.get();
        final ArmeriaCentralDogmaBuilder b = new ArmeriaCentralDogmaBuilder();
        b.host("foo");
        assertThat(b.endpoint()).isEqualTo(Endpoint.of("foo", 36462));

        // No new group should be registered.
        assertThat(AbstractArmeriaCentralDogmaBuilder.nextAnonymousGroupId).hasValue(id);
        assertThat(EndpointGroupRegistry.get("centraldogma-anonymous-" + id)).isNull();
    }

    @Test
    public void buildingWithMultipleHosts() {
        final long id = AbstractArmeriaCentralDogmaBuilder.nextAnonymousGroupId.get();
        final String groupName = "centraldogma-anonymous-" + id;
        try {
            final ArmeriaCentralDogmaBuilder b = new ArmeriaCentralDogmaBuilder();
            b.host("foo", 1);
            b.host("bar", 2);
            final Endpoint endpoint = b.endpoint();
            assertThat(endpoint.isGroup()).isTrue();
            assertThat(endpoint.groupName()).isEqualTo(groupName);

            assertThat(AbstractArmeriaCentralDogmaBuilder.nextAnonymousGroupId).hasValue(id + 1);

            final List<Endpoint> endpoints =
                    EndpointGroupRegistry.get(groupName).endpoints();
            assertThat(endpoints).isNotNull();
            assertThat(endpoints).containsExactlyInAnyOrder(
                    Endpoint.of("foo", 1), Endpoint.of("bar", 2));
        } finally {
            EndpointGroupRegistry.unregister(groupName);
        }
    }

    private static final class ArmeriaCentralDogmaBuilder
            extends AbstractArmeriaCentralDogmaBuilder<ArmeriaCentralDogmaBuilder> {}
}