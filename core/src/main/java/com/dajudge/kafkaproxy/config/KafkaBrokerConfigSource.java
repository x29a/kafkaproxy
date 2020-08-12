/*
 * Copyright 2019-2020 The kafkaproxy developers (see CONTRIBUTORS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.dajudge.kafkaproxy.config;

import com.dajudge.kafkaproxy.ca.ClientCertificateStrategy;
import com.dajudge.kafkaproxy.ca.NullCertificateAuthorityFactory;
import com.dajudge.proxybase.ca.ClientCertCertificateAuthority.ClientCertificateConfig;
import com.dajudge.proxybase.config.DownstreamSslConfig;

public class KafkaBrokerConfigSource implements ConfigSource<KafkaBrokerConfigSource.KafkaBrokerConfig> {
    private static final String KAFKA_SSL_PREFIX = PREFIX + "KAFKA_SSL_";
    private static final String ENV_KAFKA_SSL_ENABLED = KAFKA_SSL_PREFIX + "ENABLED";
    private static final String ENV_KAFKA_SSL_TRUSTSTORE_LOCATION = KAFKA_SSL_PREFIX + "TRUSTSTORE_LOCATION";
    private static final String ENV_KAFKA_SSL_TRUSTSTORE_PASSWORD = KAFKA_SSL_PREFIX + "TRUSTSTORE_PASSWORD";
    private static final String ENV_KAFKA_SSL_TRUSTSTORE_TYPE = KAFKA_SSL_PREFIX + "TRUSTSTORE_TYPE";
    private static final String ENV_KAFKA_SSL_KEYSTORE_LOCATION = KAFKA_SSL_PREFIX + "KEYSTORE_LOCATION";
    private static final String ENV_KAFKA_SSL_KEYSTORE_PASSWORD = KAFKA_SSL_PREFIX + "KEYSTORE_PASSWORD";
    private static final String ENV_KAFKA_SSL_KEYSTORE_TYPE = KAFKA_SSL_PREFIX + "KEYSTORE_TYPE";
    private static final String ENV_KAFKA_SSL_KEY_PASSWORD = KAFKA_SSL_PREFIX + "KEY_PASSWORD";
    private static final String ENV_KAFKA_CLIENT_CERT_STRATEGY = KAFKA_SSL_PREFIX + "CLIENT_CERT_STRATEGY";
    private static final String ENV_KAFKA_SSL_VERIFY_HOSTNAME = KAFKA_SSL_PREFIX + "VERIFY_HOSTNAME";
    private static final String ENV_KAFKA_SSL_CERTIFICATE_FACTORY = KAFKA_SSL_PREFIX + "CERTIFICATE_FACTORY";
    private static final boolean DEFAULT_KAFKA_SSL_ENABLED = false;
    private static final boolean DEFAULT_KAFKA_SSL_VERIFY_HOSTNAME = true;
    private static final String DEFAULT_KAFKA_SSL_TRUSTSTORE_TYPE = "jks";
    private static final String DEFAULT_KAFKA_SSL_KEYSTORE_TYPE = "jks";
    private static final String DEFAULT_CERTIFICATE_FACTORY = "null";

    @Override
    public Class<KafkaBrokerConfig> getConfigClass() {
        return KafkaBrokerConfig.class;
    }

    @Override
    public KafkaBrokerConfig parse(final Environment environment) {
        final boolean enabled = environment.requiredBoolean(ENV_KAFKA_SSL_ENABLED, DEFAULT_KAFKA_SSL_ENABLED);
        if (!enabled) {
            return KafkaBrokerConfig.DISABLED;
        }
        final DownstreamSslConfig downstreamConfig = new DownstreamSslConfig(
                enabled,
                environment.optionalFile(ENV_KAFKA_SSL_TRUSTSTORE_LOCATION).orElse(null),
                environment.optionalString(ENV_KAFKA_SSL_TRUSTSTORE_PASSWORD).orElse(null),
                environment.optionalString(ENV_KAFKA_SSL_TRUSTSTORE_TYPE).orElse(DEFAULT_KAFKA_SSL_TRUSTSTORE_TYPE),
                environment.requiredBoolean(ENV_KAFKA_SSL_VERIFY_HOSTNAME, DEFAULT_KAFKA_SSL_VERIFY_HOSTNAME)
        );
        final ClientCertificateConfig clientCertConfig = new ClientCertificateConfig(
                environment.optionalFile(ENV_KAFKA_SSL_KEYSTORE_LOCATION).orElse(null),
                environment.optionalString(ENV_KAFKA_SSL_KEYSTORE_PASSWORD).orElse(null),
                environment.optionalString(ENV_KAFKA_SSL_KEY_PASSWORD).orElse(null),
                environment.optionalString(ENV_KAFKA_SSL_KEYSTORE_TYPE).orElse(DEFAULT_KAFKA_SSL_KEYSTORE_TYPE)
        );
        return new KafkaBrokerConfig(
                downstreamConfig,
                clientCertConfig,
                environment.requiredString(ENV_KAFKA_SSL_CERTIFICATE_FACTORY, DEFAULT_CERTIFICATE_FACTORY),
                ClientCertificateStrategy.valueOf(environment.requiredString(ENV_KAFKA_CLIENT_CERT_STRATEGY, "NONE"))
        );
    }

    public static class KafkaBrokerConfig {
        private final DownstreamSslConfig downstreamConfig;
        private final ClientCertificateConfig clientCertificateConfig;
        private final String certificateFactory;
        private final ClientCertificateStrategy clientCertificateStrategy;

        public static final KafkaBrokerConfig DISABLED = new KafkaBrokerConfig(
                DownstreamSslConfig.NO_SSL,
                ClientCertificateConfig.DISABLED,
                NullCertificateAuthorityFactory.NAME,
                ClientCertificateStrategy.NONE
        );

        KafkaBrokerConfig(
                final DownstreamSslConfig downstreamConfig,
                final ClientCertificateConfig clientCertificateConfig,
                final String certificateFactory,
                final ClientCertificateStrategy clientCertificateStrategy
        ) {
            this.downstreamConfig = downstreamConfig;
            this.clientCertificateConfig = clientCertificateConfig;
            this.certificateFactory = certificateFactory;
            this.clientCertificateStrategy = clientCertificateStrategy;
        }

        public DownstreamSslConfig getDownstreamConfig() {
            return downstreamConfig;
        }

        public ClientCertificateConfig getClientCertificateConfig() {
            return clientCertificateConfig;
        }

        public String getCertificateFactory() {
            return certificateFactory;
        }

        public ClientCertificateStrategy getClientCertificateStrategy() {
            return clientCertificateStrategy;
        }

    }
}
