package mx.grupocorasa.sat.cfdi.v3;

import mx.grupocorasa.sat.cfd._30.Comprobante;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CFDv30 extends CFDv3 {

    private final String XSLT = "/xslt/cfd/3/cadenaoriginal_3_0/cadenaoriginal_3_0.xslt";
    private final String BASE_CONTEXT = "mx.grupocorasa.sat.cfd._30";
    private final List<String> schemaLocations = Stream.of("http://www.sat.gob.mx/cfd/3 http://www.sat.gob.mx/sitio_internet/cfd/3/cfdv3.xsd").collect(Collectors.toList());
    private final String[] XSD = new String[]{
            "/xsd/cfd/3/cfdv3.xsd",
            "/xsd/common/TimbreFiscalDigital/TimbreFiscalDigital.xsd",
            "/xsd/common/detallista/detallista.xsd",
            "/xsd/common/divisas/divisas.xsd",
            "/xsd/common/donat/donat.xsd",
            "/xsd/common/ecb/ecb.xsd",
            "/xsd/common/ecc/ecc.xsd",
            "/xsd/common/iedu/iedu.xsd",
            "/xsd/common/implocal/implocal.xsd",
            "/xsd/common/terceros/terceros.xsd"
    };
    private final Comprobante document;
    private final JAXBContext context;

    public CFDv30(InputStream in, String... contexts) throws Exception {
        this.document = (Comprobante) load(in, contexts);
        this.context = getContext(document, contexts);
    }

    public CFDv30(Comprobante comprobante, String... contexts) throws Exception {
        this.context = getContext(comprobante, contexts);
        this.document = copy(comprobante);
    }

    public static Comprobante newComprobante(InputStream in) throws Exception {
        return new CFDv30(in).document;
    }

    @Override
    public String getBaseContext() {
        return BASE_CONTEXT;
    }

    @Override
    public String[] getXSD() {
        return XSD;
    }

    @Override
    public String getXSLT() {
        return XSLT;
    }

    @Override
    public void addSchemaLocation(String uri) {
        schemaLocations.add(uri);
    }

    @Override
    public JAXBSource getJAXBSource() throws JAXBException {
        return new JAXBSource(context, document);
    }

    @Override
    public int getYear() {
        return document.getFecha().getYear();
    }

    @Override
    public String getCertificadoString() {
        return document.getCertificado();
    }

    @Override
    public String getSelloString() {
        return document.getSello();
    }

    @Override
    public Marshaller createMarshaller() throws JAXBException {
        return context.createMarshaller();
    }

    @Override
    public List<String> getSchemaLocation() {
        return schemaLocations;
    }

    @Override
    public Object getComprobanteDocument() {
        return document;
    }

    @Override
    public void sellar(PrivateKey key, X509Certificate cert) throws Exception {
        document.setSello(getSignature(key));
        document.setCertificado(Base64.getEncoder().encodeToString(cert.getEncoded()));
        document.setNoCertificado(new String(cert.getSerialNumber().toByteArray()));
    }

    private List<String> getComprobanteContexts(Comprobante comprobante) throws IOException {
        final List<String> contexts = new ArrayList<>();
        if (comprobante != null && comprobante.getComplemento() != null && comprobante.getComplemento().getAny() != null && comprobante.getComplemento().getAny().size() > 0) {
            for (Object c : comprobante.getComplemento().getAny()) {
                defineComprobanteContext(c, contexts);
            }
        }
        return contexts;
    }

    private JAXBContext getContext(Comprobante comprobante, String[] addendas) throws Exception {
        final List<String> contexts = getComprobanteContexts(comprobante);
        if (addendas != null && addendas.length > 0)
            contexts.addAll(Arrays.asList(addendas));
        contexts.add(0, BASE_CONTEXT);
        if (!contextMap.containsKey(contexts)) {
            JAXBContext context = JAXBContext.newInstance(JOINER.join(contexts));
            contextMap.put(contexts, context);
        }
        return contextMap.get(contexts);
    }

    public Comprobante sellarComprobante(PrivateKey key, X509Certificate cert) throws Exception {
        sellar(key, cert);
        return getComprobante();
    }

    private Comprobante getComprobante() throws Exception {
        return copy(document);
    }

    private Comprobante copy(Comprobante comprobante) throws Exception {
        Document doc = getDocument();
        Marshaller m = createMarshaller();
        m.marshal(comprobante, doc);
        Unmarshaller u = context.createUnmarshaller();
        return (Comprobante) u.unmarshal(doc);
    }

}