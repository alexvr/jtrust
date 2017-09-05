/*
 * Java Trust Project.
 * Copyright (C) 2009 FedICT.
 * Copyright (C) 2014 e-Contract.be BVBA.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */

package be.fedict.trust.constraints;

import java.io.ByteArrayInputStream;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.PolicyInformation;

import be.fedict.trust.linker.TrustLinkerResultException;
import be.fedict.trust.linker.TrustLinkerResultReason;

/**
 * Certificate Policies certificate constraint implementation.
 * 
 * @author Frank Cornelis
 * 
 */
public class CertificatePoliciesCertificateConstraint implements
		CertificateConstraint {

	private static final Log LOG = LogFactory
			.getLog(CertificatePoliciesCertificateConstraint.class);

	private final Set<String> certificatePolicies;

	/**
	 * Default constructor.
	 */
	public CertificatePoliciesCertificateConstraint() {
		this.certificatePolicies = new HashSet<>();
	}

	/**
	 * Adds a certificate policy OID to this certificate constraint.
	 * 
	 * @param certificatePolicy
	 */
	public void addCertificatePolicy(final String certificatePolicy) {
		this.certificatePolicies.add(certificatePolicy);
	}

	@Override
	public void check(final X509Certificate certificate)
			throws TrustLinkerResultException, Exception {
		final byte[] extensionValue = certificate
				.getExtensionValue(Extension.certificatePolicies.getId());
		if (null == extensionValue) {
			throw new TrustLinkerResultException(
					TrustLinkerResultReason.CONSTRAINT_VIOLATION,
					"missing certificate policies X509 extension");
		}
		try (final ASN1InputStream extensionStream = new ASN1InputStream(new ByteArrayInputStream(extensionValue))) {
			final DEROctetString octetString = (DEROctetString) extensionStream.readObject();
			try (final ASN1InputStream octetStream = new ASN1InputStream(octetString.getOctets())) {
				final ASN1Sequence certPolicies = (ASN1Sequence) octetStream.readObject();
				final Enumeration<?> certPoliciesEnum = certPolicies.getObjects();
				while (certPoliciesEnum.hasMoreElements()) {
					final PolicyInformation policyInfo = PolicyInformation.getInstance(certPoliciesEnum.nextElement());
					final ASN1ObjectIdentifier policyOid = policyInfo.getPolicyIdentifier();
					final String policyId = policyOid.getId();
					LOG.debug("present policy OID: " + policyId);
					if (this.certificatePolicies.contains(policyId)) {
						LOG.debug("matching certificate policy OID: " + policyId);
						return;
					}
				}
			}
		}
		throw new TrustLinkerResultException(
				TrustLinkerResultReason.CONSTRAINT_VIOLATION,
				"required policy OID not present");
	}
}
