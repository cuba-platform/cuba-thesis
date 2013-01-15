/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.portal.restapi;

import com.haulmont.chile.core.datatypes.impl.StringDatatype;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.entity.BaseUuidEntity;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.security.entity.EntityAttrAccess;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.cuba.security.global.UserSession;
import org.w3c.dom.*;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSParserFilter;
import org.w3c.dom.traversal.NodeFilter;

import javax.activation.MimeType;
import javax.persistence.Embedded;
import javax.persistence.Id;
import javax.persistence.Version;
import javax.servlet.http.HttpServletResponse;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.*;

/**
 * @version $Id$
 */
public class XMLConvertor implements Convertor {
    public static final MimeType MIME_TYPE_XML;
    public static final String MIME_STR = "text/xml;charset=UTF-8";

    public static final String ELEMENT_INSTANCE = "instance";
    public static final String ELEMENT_URI = "uri";
    public static final String ELEMENT_REF = "ref";
    public static final String ELEMENT_NULL_REF = "null";
    public static final String ELEMENT_MEMBER = "member";
    public static final String ATTR_ID = "id";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_VERSION = "version";
    public static final String ATTR_NULL = "null";
    public static final String ATTR_MEMBER_TYPE = "member-type";
    public static final String NULL_VALUE = "null";

    private static final String EMPTY_TEXT = " ";
    public static final char DASH = '-';
    public static final char UNDERSCORE = '_';

    public static final String ROOT_ELEMENT_INSTANCE = "instances";

    private static DocumentBuilder _builder;

    public static final String MAPPING_ROOT_ELEMENT_INSTANCE = "mapping";
    public static final String PAIR_ELEMENT = "pair";

    private static final Transformer _transformer;
    private static LSParser requestConfigParser;
    private static DOMImplementationLS lsImpl;

    static {
        try {
            MIME_TYPE_XML = new MimeType(MIME_STR);

            _builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            lsImpl = (DOMImplementationLS) registry.getDOMImplementation("LS");
            requestConfigParser = lsImpl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS,
                    null);

            // Set options on the parser
            DOMConfiguration config = requestConfigParser.getDomConfig();
            config.setParameter("validate", Boolean.TRUE);
            config.setParameter("element-content-whitespace", Boolean.FALSE);
            config.setParameter("comments", Boolean.FALSE);
            requestConfigParser.setFilter(new LSParserFilter() {
                public short startElement(Element elementArg) {
                    return LSParserFilter.FILTER_ACCEPT;
                }

                public short acceptNode(Node nodeArg) {
                    return "".equals(nodeArg.getTextContent().trim()) ?
                            LSParserFilter.FILTER_REJECT :
                            LSParserFilter.FILTER_ACCEPT;
                }

                public int getWhatToShow() {
                    return NodeFilter.SHOW_TEXT;
                }
            });

            _transformer = TransformerFactory.newInstance().newTransformer();
            _transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            _transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            _transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
            _transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            _transformer.setOutputProperty(OutputKeys.INDENT, "no");
            _transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public MimeType getMimeType() {
        return MIME_TYPE_XML;
    }

    public Document process(Entity entity, MetaClass metaclass, String requestURI)
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Element root = newDocument(ROOT_ELEMENT_INSTANCE);
        encodeEntityInstance(new HashSet<Entity>(), entity, root, false, metaclass);
        Document doc = root.getOwnerDocument();
        decorate(doc, requestURI);
        return doc;
    }

    public Document process(List<Entity> entities, MetaClass metaClass, String requestURI)
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Element root = newDocument(ROOT_ELEMENT_INSTANCE);
        for (Entity entity : entities) {
            encodeEntityInstance(new HashSet(), entity, root, false, metaClass);
        }
        Document doc = root.getOwnerDocument();
        decorate(doc, requestURI);
        return doc;
    }

    public Object process(Map<Entity, Entity> entityMap, String requestURI)
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Element root = newDocument(MAPPING_ROOT_ELEMENT_INSTANCE);
        Document doc = root.getOwnerDocument();
        for (Map.Entry<Entity, Entity> entry : entityMap.entrySet()) {
            Element pair = doc.createElement(PAIR_ELEMENT);
            root.appendChild(pair);
            encodeEntityInstance(
                    new HashSet(), entry.getKey(),
                    pair, false,
                    getMetaClass(entry.getKey())
            );
            encodeEntityInstance(
                    new HashSet(), entry.getValue(),
                    pair, false,
                    getMetaClass(entry.getValue())
            );
        }
        return doc;
    }

    public void write(HttpServletResponse response, Object o) throws IOException {
        Document doc = (Document) o;
        response.setContentType(MIME_STR);
        try {
            _transformer.transform(new DOMSource(doc), new StreamResult(response.getOutputStream()));
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public CommitRequest parseCommitRequest(String content) {
        try {
            LSInput lsInput = lsImpl.createLSInput();
            lsInput.setStringData(content);
            Document commitRequestDoc = requestConfigParser.parse(lsInput);
            Node rootNode = commitRequestDoc.getFirstChild();
            if (!"CommitRequest".equals(rootNode.getNodeName()))
                throw new IllegalArgumentException("Not a CommitRequest xml passed: " + rootNode.getNodeName());

            CommitRequest result = new CommitRequest();

            NodeList children = rootNode.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                String childNodeName = child.getNodeName();
                if ("commitInstances".equals(childNodeName)) {
                    NodeList entitiesNodeList = child.getChildNodes();

                    Set<String> commitIds = new HashSet<>(entitiesNodeList.getLength());
                    for (int j = 0; j < entitiesNodeList.getLength(); j++) {
                        Node idNode = entitiesNodeList.item(j).getAttributes().getNamedItem("id");
                        if (idNode == null)
                            continue;

                        String id = idNode.getTextContent();
                        if (id.startsWith("NEW-"))
                            id = id.substring(id.indexOf('-') + 1);
                        commitIds.add(id);
                    }

                    result.setCommitIds(commitIds);
                    result.setCommitInstances(parseNodeList(result, entitiesNodeList));
                } else if ("removeInstances".equals(childNodeName)) {
                    NodeList entitiesNodeList = child.getChildNodes();

                    List removeInstances = parseNodeList(result, entitiesNodeList);
                    result.setRemoveInstances(removeInstances);
                } else if ("softDeletion".equals(childNodeName)) {
                    result.setSoftDeletion(Boolean.parseBoolean(child.getTextContent()));
                }
            }
            return result;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private List parseNodeList(CommitRequest commitRequest, NodeList entitiesNodeList) throws InstantiationException, IllegalAccessException, InvocationTargetException, IntrospectionException, ParseException {
        List entities = new ArrayList(entitiesNodeList.getLength());
        for (int j = 0; j < entitiesNodeList.getLength(); j++) {
            Node entityNode = entitiesNodeList.item(j);
            if (ELEMENT_INSTANCE.equals(entityNode.getNodeName())) {
                InstanceRef ref = commitRequest.parseInstanceRefAndRegister(getIdAttribute(entityNode));
                MetaClass metaClass = ref.getMetaClass();
                Object instance = ref.getInstance();
                parseEntity(commitRequest, instance, metaClass, entityNode);
                entities.add(instance);
            }
        }
        return entities;
    }

    private void parseEntity(CommitRequest commitRequest, Object bean, MetaClass metaClass, Node node)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, IntrospectionException, ParseException {
        MetadataTools metadataTools = AppBeans.get(MetadataTools.class);
        NodeList fields = node.getChildNodes();
        for (int i = 0; i < fields.getLength(); i++) {
            Node fieldNode = fields.item(i);
            String fieldName = getFieldName(fieldNode);
            MetaProperty property = metaClass.getProperty(fieldName);

            if (!attrModifyPermitted(metaClass, property.getName()))
                continue;

            if (metadataTools.isTransient(bean, fieldName))
                continue;

            String xmlValue = fieldNode.getTextContent();
            if (isNullValue(fieldNode)) {
                setNullField(bean, fieldName);
            }

            Object value;

            switch (property.getType()) {
                case DATATYPE:
                case ENUM:
                    if (property.getAnnotatedElement().isAnnotationPresent(Id.class)) {
                        // it was parsed in the beginning
                        continue;
                    }

                    String typeName = property.getRange().<Object>asDatatype().getName();

                    if (property.getType() == MetaProperty.Type.DATATYPE)
                        if (!StringDatatype.NAME.equals(typeName) && "null".equals(xmlValue))
                            value = null;
                        else
                            value = property.getRange().<Object>asDatatype().parse(xmlValue);
                    else
                        value = property.getRange().asEnumeration().parse(xmlValue);

                    setField(bean, fieldName, value);
                    break;
                case COMPOSITION:
                case ASSOCIATION: {
                    if ("null".equals(xmlValue)) {
                        setField(bean, fieldName, null);
                        break;
                    }
                    MetaClass propertyMetaClass = propertyMetaClass(property);
                    //checks if the user permitted to read and update a property
                    if (!updatePermitted(propertyMetaClass) && !readPermitted(propertyMetaClass))
                        break;

                    if (!property.getRange().getCardinality().isMany()) {
                        if (property.getAnnotatedElement().isAnnotationPresent(Embedded.class)) {
                            MetaClass embeddedMetaClass = property.getRange().asClass();
                            value = embeddedMetaClass.createInstance();
                            parseEntity(commitRequest, value, embeddedMetaClass, fieldNode);
                        } else {
                            String id = getRefId(fieldNode);

                            //reference to an entity that also a commit instance
                            //will be registered later
                            if (commitRequest.getCommitIds().contains(id)) {
                                EntityLoadInfo loadInfo = EntityLoadInfo.parse(id);
                                BaseUuidEntity ref = loadInfo.getMetaClass().createInstance();
                                ref.setValue("id", loadInfo.getId());
                                setField(bean, fieldName, ref);
                                break;
                            }

                            value = parseEntityReference(fieldNode, commitRequest);
                        }
                        setField(bean, fieldName, value);
                    } else {
                        NodeList memberNodes = fieldNode.getChildNodes();
                        Collection<Object> members = property.getRange().isOrdered() ? new ArrayList<Object>() : new HashSet<Object>();
                        for (int memberIndex = 0; memberIndex < memberNodes.getLength(); memberIndex++) {
                            Node memberNode = memberNodes.item(memberIndex);
                            members.add(parseEntityReference(memberNode, commitRequest));
                        }
                        setField(bean, fieldName, members);
                    }
                    break;
                }
                default:
                    throw new IllegalStateException("Unknown property type");
            }
        }
    }

    private String getRefId(Node refNode) {
        Node childNode = refNode.getFirstChild();
        do {
            if (ELEMENT_REF.equals(childNode.getNodeName())) {
                Node idNode = childNode.getAttributes().getNamedItem(ATTR_ID);
                return idNode != null ? idNode.getTextContent() : null;
            }
            childNode = childNode.getNextSibling();
        } while (childNode != null);
        return null;
    }

    private Object parseEntityReference(Node node, CommitRequest commitRequest)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, IntrospectionException {
        Node childNode = node.getFirstChild();
        if (ELEMENT_NULL_REF.equals(childNode.getNodeName())) {
            return null;
        }

        InstanceRef ref = commitRequest.parseInstanceRefAndRegister(getIdAttribute(childNode));
        return ref.getInstance();
    }

    private void setField(Object result, String fieldName, Object value) throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        new PropertyDescriptor(fieldName, result.getClass()).
                getWriteMethod().invoke(result, value);
    }

    private void setNullField(Object bean, String fieldName) throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        setField(bean, fieldName, null);
    }

    private String getFieldName(Node fieldNode) {
        return getAttributeValue(fieldNode, ATTR_NAME);
    }

    private String getIdAttribute(Node node) {
        return getAttributeValue(node, ATTR_ID);
    }

    private String getAttributeValue(Node node, String name) {
        return node.getAttributes().getNamedItem(name).getNodeValue();
    }

    /**
     * Create a new document with the given tag as the root element.
     *
     * @param rootTag the tag of the root element
     * @return the document element of a new document
     */

    public Element newDocument(String rootTag) {
        Document doc = _builder.newDocument();
        Element root = doc.createElement(rootTag);
        doc.appendChild(root);
        String[] nvpairs = new String[]{
                "xmlns:xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI,
//                "xsi:noNamespaceSchemaLocation", INSTANCE_XSD,
                ATTR_VERSION, "1.0",
        };
        for (int i = 0; i < nvpairs.length; i += 2) {
            root.setAttribute(nvpairs[i], nvpairs[i + 1]);
        }
        return root;
    }


    Document decorate(Document doc, String uri) {
        Element root = doc.getDocumentElement();
        Element instance = (Element) root.getElementsByTagName(ELEMENT_INSTANCE).item(0);
        Element uriElement = doc.createElement(ELEMENT_URI);
        uriElement.setTextContent(uri == null ? NULL_VALUE : uri);
        root.insertBefore(uriElement, instance);
        return doc;
    }

    /**
     * Encodes the closure of a persistent instance into a XML element.
     *
     * @param visited
     * @param entity    the managed instance to be encoded. Can be null.
     * @param parent    the parent XML element to which the new XML element be added. Must not be null. Must be
     *                  owned by a document.
     * @param isRef
     * @param metaClass @return the new element. The element has been appended as a child to the given parent in this method.
     */
    private Element encodeEntityInstance(HashSet<Entity> visited, final Entity entity, final Element parent,
                                         boolean isRef, MetaClass metaClass)
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        if (!readPermitted(metaClass))
            return null;

        if (parent == null)
            throw new NullPointerException("No parent specified");

        Document doc = parent.getOwnerDocument();
        if (doc == null)
            throw new NullPointerException("No document specified");

        if (entity == null) {
            return encodeRef(parent, entity);
        }

        isRef |= !visited.add(entity);

        if (isRef) {
            return encodeRef(parent, entity);
        }
        Element root = doc.createElement(ELEMENT_INSTANCE);
        parent.appendChild(root);
        root.setAttribute(ATTR_ID, ior(entity));

        MetadataTools metadataTools = AppBeans.get(MetadataTools.class);
        List<MetaProperty> properties = ConvertorHelper.getOrderedProperties(metaClass);
        for (MetaProperty property : properties) {
            Element child;
            if (metadataTools.isTransient(entity, property.getName()))
                continue;

            if (!attrViewPermitted(metaClass, property.getName()))
                continue;

            Object value = entity.getValue(property.getName());
            switch (property.getType()) {
                case DATATYPE:
                    String nodeType;
                    if (property.getAnnotatedElement().isAnnotationPresent(Id.class)) {
                        continue;
                    } else if (property.getAnnotatedElement().isAnnotationPresent(Version.class)) {
                        nodeType = "version";
                    } else {
                        nodeType = "basic";
                    }
                    child = doc.createElement(nodeType);
                    child.setAttribute(ATTR_NAME, property.getName());
                    if (value == null) {
                        encodeNull(child);
                    } else {
                        String str = property.getRange().<Object>asDatatype().format(value);
                        encodeBasic(child, str, property.getJavaType());
                    }
                    break;
                case ENUM:
                    child = doc.createElement("enum");
                    child.setAttribute(ATTR_NAME, property.getName());
                    if (value == null) {
                        encodeNull(child);
                    } else {
                        String str = property.getRange().asEnumeration().format(value);
                        encodeBasic(child, str, property.getJavaType());
                    }
                    break;
                case COMPOSITION:
                case ASSOCIATION: {
                    MetaClass meta = propertyMetaClass(property);
                    //checks if the user permitted to read a property
                    if (!readPermitted(meta)) {
                        child = null;
                        break;
                    }

                    if (!property.getRange().getCardinality().isMany()) {
                        boolean isEmbedded = property.getAnnotatedElement().isAnnotationPresent(Embedded.class);
                        child = doc.createElement(isEmbedded ?
                                "embedded" :
                                property.getRange().getCardinality().name().replace(UNDERSCORE, DASH).toLowerCase()
                        );
                        child.setAttribute(ATTR_NAME, property.getName());
                        if (isEmbedded) {
                            encodeEntityInstance(visited, (Entity) value, child, false, property.getRange().asClass());
                        } else {
                            encodeEntityInstance(visited, (Entity) value, child, false, property.getRange().asClass());
                        }
                    } else {
                        child = doc.createElement(getCollectionReferenceTag(property));
                        child.setAttribute(ATTR_NAME, property.getName());
                        child.setAttribute(ATTR_MEMBER_TYPE, typeOfEntityProperty(property));
                        if (value == null) {
                            encodeNull(child);
                            break;
                        }
                        Collection<?> members = (Collection<?>) value;
                        for (Object o : members) {
                            Element member = doc.createElement(ELEMENT_MEMBER);
                            child.appendChild(member);
                            if (o == null) {
                                encodeNull(member);
                            } else {
                                encodeEntityInstance(visited, (Entity) o, member, true, property.getRange().asClass());
                            }
                        }
                    }
                    break;
                }
                default:
                    throw new IllegalStateException("Unknown property type");
            }

            if (child != null) {
                root.appendChild(child);
            }
        }
        return root;
    }

    private String typeOfEntityProperty(MetaProperty property) {
        return property.getRange().asClass().getName();
    }

    private MetaClass propertyMetaClass(MetaProperty property) {
        return property.getRange().asClass();
    }

    /**
     * Sets the given value element as null. The <code>null</code> attribute is set to true.
     *
     * @param element the XML element to be set
     */
    private void encodeNull(Element element) {
        element.setAttribute(ATTR_NULL, "true");
    }

    private boolean isNullValue(Node fieldNode) {
        Node nullAttr = fieldNode.getAttributes().getNamedItem(ATTR_NULL);
        return nullAttr == null ?
                false :
                "true".equals(nullAttr.getNodeValue());
    }

    private Element encodeRef(Element parent, Entity entity) {
        Element ref = parent.getOwnerDocument().createElement(entity == null ? ELEMENT_NULL_REF : ELEMENT_REF);
        if (entity != null)
            ref.setAttribute(ATTR_ID, ior(entity));

        // IMPORTANT: for xml transformer not to omit the closing tag, otherwise dojo is confused
        ref.setTextContent(EMPTY_TEXT);
        parent.appendChild(ref);
        return ref;
    }


    /**
     * Sets the given value element. The <code>type</code> is set to the given runtime type.
     * String form of the given object is set as the text content.
     *
     * @param element     the XML element to be set
     * @param obj         value of the element. Never null.
     * @param runtimeType attribute type
     */
    private void encodeBasic(Element element, Object obj, Class<?> runtimeType) {
        element.setTextContent(obj == null ? NULL_VALUE : obj.toString());
    }

    String ior(Entity entity) {
        return EntityLoadInfo.create(entity).toString();
    }

    String typeOf(Class<?> cls) {
        return cls.getSimpleName();
    }

    private String getCollectionReferenceTag(MetaProperty property) {
        return property.getRange().getCardinality().name().replace(UNDERSCORE, DASH).toLowerCase();
    }

    private MetaClass getMetaClass(Entity entity) {
        return MetadataProvider.getSession().getClass(entity.getClass());
    }

    private boolean attrViewPermitted(MetaClass metaClass, String property) {
        return attrPermitted(metaClass, property, EntityAttrAccess.VIEW);
    }

    private boolean attrModifyPermitted(MetaClass metaClass, String property) {
        return attrPermitted(metaClass, property, EntityAttrAccess.MODIFY);
    }

    private boolean attrPermitted(MetaClass metaClass, String property, EntityAttrAccess entityAttrAccess) {
        UserSession session = UserSessionProvider.getUserSession();
        return session.isEntityAttrPermitted(metaClass, property, entityAttrAccess);
    }

    private boolean readPermitted(MetaClass metaClass) {
        return entityOpPermitted(metaClass, EntityOp.READ);
    }

    private boolean updatePermitted(MetaClass metaClass) {
        return entityOpPermitted(metaClass, EntityOp.UPDATE);
    }
    
    private boolean entityOpPermitted(MetaClass metaClass, EntityOp entityOp) {
        UserSession session = UserSessionProvider.getUserSession();
        return session.isEntityOpPermitted(metaClass, entityOp);
    }

}
