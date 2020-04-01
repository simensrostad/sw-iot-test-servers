/*******************************************************************************
 * Copyright (c) 2015, 2017 Institute for Pervasive Computing, ETH Zurich and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *    Matthias Kovatsch - creator and main architect
 *    Achim Kraus (Bosch Software Innovations GmbH) - use credentials util to setup
 *                                                    DtlsConnectorConfig.Builder.
 *    Bosch Software Innovations GmbH - migrate to SLF4J
 ******************************************************************************/
package nordic.server;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.network.interceptors.MessageTracer;
import nordic.server.CredentialsUtil.Mode;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import nordic.server.resources.StorageResource;

public class SecureServer {

	public static final List<Mode> SUPPORTED_MODES = Arrays
			.asList(new Mode[] { Mode.PSK, Mode.ECDHE_PSK, Mode.RPK, Mode.X509, Mode.NO_AUTH, Mode.WANT_AUTH, Mode.NO_DTLS });

	// allows configuration via Californium.properties
	public static final int DTLS_PORT = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_SECURE_PORT);

	public static void main(String[] args) {

		System.out.println("Usage: java -jar ... [PSK] [ECDHE_PSK] [RPK] [X509] [NO_AUTH] [WANT_AUTH] [NO_DTLS]  ");
		System.out.println("Default :            [PSK] [ECDHE_PSK] [RPK] [X509]");
		
		CoapServer server = new CoapServer();
		server.add(new StorageResource("iot_publisher"));
		
		boolean dtls = true;
		
		if(SUPPORTED_MODES.contains(Mode.NO_DTLS)) {
			dtls = false;
		}
		
		if(dtls) {
			DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder();
			CredentialsUtil.setupCid(args, builder);
			builder.setAddress(new InetSocketAddress(DTLS_PORT));
			List<Mode> modes = CredentialsUtil.parse(args, CredentialsUtil.DEFAULT_SERVER_MODES, SUPPORTED_MODES);
			CredentialsUtil.setupCredentials(builder, CredentialsUtil.SERVER_NAME, modes);
			DTLSConnector connector = new DTLSConnector(builder.build());
			CoapEndpoint.Builder coapBuilder = new CoapEndpoint.Builder();
			coapBuilder.setConnector(connector);
			server.addEndpoint(coapBuilder.build());
		}

		server.start();

		if(dtls) {
			// add special intercepter for message traces
			for (Endpoint ep : server.getEndpoints()) {
				ep.addInterceptor(new MessageTracer());
			}
		}

		System.out.println("Secure CoAP server powered by Scandium (Sc) is listening on port " + DTLS_PORT);
	}
}
