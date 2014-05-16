package org.uppermodel.tools;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.SimpleIRIMapper;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * A helper class for compilation.
 * 
 * @author Daniel Couto Vale <danielvale@uni-bremen.de>
 */
public class CompileHelper {

	static final Document parseFile(String fileName) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
		Document document = documentBuilder.parse(new File(fileName));
		return document;
	}

	static final Document makeDocument() throws ParserConfigurationException {
		DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
		Document document = documentBuilder.newDocument();
		return document;
	}

	/**
	 * Save document to a file in XML format
	 * 
	 * @param document the document to save
	 * @param fileName the name of the file
	 * @throws IOException
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerConfigurationException
	 * @throws TransformerException
	 */
	static final void saveDocument(Document document, String fileName) throws IOException,
			TransformerFactoryConfigurationError,
			TransformerConfigurationException, TransformerException {
		DOMSource source = new DOMSource(document);
		File file = new File(fileName);
		file.createNewFile();
		StreamResult result = new StreamResult(file);
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		transformer.transform(source, result);
	}

	static final OntologyBundle makeOntologyBundle(String ss) throws URISyntaxException,
			OWLOntologyCreationException {
		IRI wordEngDocumentIri = IRI.create(CompileIntoKpmlNetwork.class.getResource("../resources/Word" + ss + ".owl"));
		IRI wordDocumentIri = IRI.create(CompileIntoKpmlNetwork.class.getResource("../resources/Word.owl"));
		IRI wordOntologyIri = IRI.create("http://www.uppermodel.org/Word");
	
		IRI wordingEngDocumentIri = IRI.create(CompileIntoKpmlNetwork.class.getResource("../resources/Wording" + ss + ".owl"));
		IRI wordingDocumentIri = IRI.create(CompileIntoKpmlNetwork.class.getResource("../resources/Wording.owl"));
		IRI wordingOntologyIri = IRI.create("http://www.uppermodel.org/Wording");
	
		// Create ontology
		OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
		ontologyManager.addIRIMapper(new SimpleIRIMapper(wordOntologyIri, wordDocumentIri));
		ontologyManager.addIRIMapper(new SimpleIRIMapper(wordingOntologyIri, wordingDocumentIri));
	
		OWLOntology wordEngOnto = ontologyManager.loadOntology(wordEngDocumentIri);
		OWLOntology wordOnto = ontologyManager.getOntology(wordOntologyIri);
		OWLOntology wordingEngOnto = ontologyManager.loadOntology(wordingEngDocumentIri);
		OWLOntology wordingOnto = ontologyManager.getOntology(wordingOntologyIri);
		
		OntologyBundle bundle = new OntologyBundle();
		bundle.wordSsWrapper = new OntologyWrapper(wordEngOnto);
		bundle.wordWrapper = new OntologyWrapper(wordOnto);
		bundle.wordingSsWrapper = new OntologyWrapper(wordingEngOnto);
		bundle.wordingWrapper = new OntologyWrapper(wordingOnto);
		return bundle;
	}

}
