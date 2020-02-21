package com.kk.dlnacling;

import org.fourthline.cling.UpnpServiceConfiguration;
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.binding.staging.MutableService;
import org.fourthline.cling.binding.xml.DescriptorBindingException;
import org.fourthline.cling.binding.xml.ServiceDescriptorBinder;
import org.fourthline.cling.binding.xml.UDA10ServiceDescriptorBinderSAXImpl;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.Service;
import org.seamless.xml.SAXParser;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.StringReader;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParserFactory;

/**
 * http://4thline.org/projects/cling/core/manual/cling-core-manual.xhtml#chapter.Android
 * https://github.com/4thline/cling/issues/249
 */
public class BrowserUpnpService extends AndroidUpnpServiceImpl {

    @Override
    protected UpnpServiceConfiguration createConfiguration() {
        return new AndroidUpnpServiceConfiguration() {
            @Override
            public int getRegistryMaintenanceIntervalMillis() {
                return 7000;
            }

            @Override
            public ServiceDescriptorBinder createServiceDescriptorBinderUDA10() {
                return new DLNAUDA10ServiceDescriptorBinderSAXImpl();
            }
        };
    }

    public static class DLNASAXParser extends SAXParser {

        protected XMLReader create() {
//            try {
//                SAXParserFactory factory = SAXParserFactory.newInstance();
//                //fix bug .see https://stackoverflow.com/questions/10837706/solve-security-issue-parsing-xml-using-sax-parser
//                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
//
//                // Configure factory to prevent XXE attacks
//                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
//                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
//                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
//                factory.setXIncludeAware(false);
//
//                factory.setNamespaceAware(true);
//
//                if (getSchemaSources() != null) {
//                    factory.setSchema(createSchema(getSchemaSources()));
//                }
//
//                XMLReader xmlReader = factory.newSAXParser().getXMLReader();
//                xmlReader.setErrorHandler(getErrorHandler());
//                return xmlReader;
//            } catch (Exception ex) {
//                throw new RuntimeException(ex);
//            }
            try {
                SAXParserFactory factory = SAXParserFactory.newInstance();

                // Configure factory to prevent XXE attacks
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

                //commenting
                //factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                //factory.setXIncludeAware(false);

                //factory.setNamespaceAware(true);

                if (getSchemaSources() != null) {
                    factory.setSchema(createSchema(getSchemaSources()));
                }

                XMLReader xmlReader = factory.newSAXParser().getXMLReader();
                xmlReader.setErrorHandler(getErrorHandler());
                return xmlReader;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public static class DLNAUDA10ServiceDescriptorBinderSAXImpl extends UDA10ServiceDescriptorBinderSAXImpl {
        private static Logger log = Logger.getLogger(ServiceDescriptorBinder.class.getName());

        @Override
        public <S extends Service> S describe(S undescribedService, String descriptorXml) throws DescriptorBindingException, ValidationException {

            if (descriptorXml == null || descriptorXml.length() == 0) {
                throw new DescriptorBindingException("Null or empty descriptor");
            }

            try {
                log.fine("Reading service from XML descriptor");

                SAXParser parser = new DLNASAXParser();

                MutableService descriptor = new MutableService();

                hydrateBasic(descriptor, undescribedService);

                new RootHandler(descriptor, parser);

                parser.parse(
                        new InputSource(
                                // TODO: UPNP VIOLATION: Virgin Media Superhub sends trailing spaces/newlines after last XML element, need to trim()
                                new StringReader(descriptorXml.trim())
                        )
                );

                // Build the immutable descriptor graph
                return (S) descriptor.build(undescribedService.getDevice());

            } catch (ValidationException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new DescriptorBindingException("Could not parse service descriptor: " + ex.toString(), ex);
            }
        }
    }
}