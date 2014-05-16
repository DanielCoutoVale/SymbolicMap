package org.uppermodel.tools;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Creates the word forms and store them in CcgMorph.xml file for OpenCCG.
 * 
 * @author Daniel Couto Vale <danielvale@uni-bremen.de>
 */
public class CompileIntoCcgMorph {

	static class XmlWord {
		String id;
		Set<String> classSet = new HashSet<String>();
		Set<String> senseSet = new HashSet<String>();
		Map<String, String> sampleMap = new HashMap<String, String>();
		public XmlWord(Element elem) {
			this.id = elem.getAttribute("id");
			NodeList classNodes = elem.getElementsByTagName("Class");
			for (int i = 0; i < classNodes.getLength(); i++) {
				Element classElem = (Element)classNodes.item(i);
				this.classSet.add(classElem.getAttribute("name"));
			}
			NodeList senseNodes = elem.getElementsByTagName("Sense");
			for (int i = 0; i < senseNodes.getLength(); i++) {
				Element senseElem = (Element)senseNodes.item(i);
				this.senseSet.add(senseElem.getAttribute("name"));
			}
			NodeList sampleNodes = elem.getElementsByTagName("Sample");
			for (int i = 0; i < sampleNodes.getLength(); i++) {
				Element sampleElem = (Element)sampleNodes.item(i);
				this.sampleMap.put(sampleElem.getAttribute("name"), sampleElem.getAttribute("value"));
			}
		}
	}

	static class CcgForm {
		XmlWord word;
		String copy;
		Set<String> classes = new HashSet<String>();
		public String wordClass;
		public String sense;
	}

	/**
	 * @param args
	 * @throws OWLOntologyCreationException
	 * @throws URISyntaxException
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 * @throws IOException
	 * @throws SAXException 
	 */
	public final static void main(String[] args) throws URISyntaxException, OWLOntologyCreationException, ParserConfigurationException, IOException, TransformerException, SAXException {

		OntologyBundle ontologyBundle = CompileHelper.makeOntologyBundle("Eng");
		Reasoner reasoner = new Reasoner(ontologyBundle.wordSsWrapper.ontology);
		DataFactoryWrapper factoryWrapper = new DataFactoryWrapper(reasoner.getDataFactory(), "Eng");
		Document document = CompileHelper.makeDocument();

		// Generate Document Element
		Element morph = document.createElement("morph");
		morph.setAttribute("name", "English");
		morph.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		morph.setAttribute("xsi:noNamespaceSchemaLocation", "../../../openccg/grammars/morph.xsd");
		document.appendChild(morph);

		Document wordEngAxioms = CompileHelper.parseFile(CompileIntoKpmlNetwork.class.getResource("../resources/WordEng.xml").getFile());
		NodeList wordNodes = wordEngAxioms.getDocumentElement().getElementsByTagName("Word");
		Set<OWLClass> formClasses = new HashSet<OWLClass>();
		for (int i = 0; i < wordNodes.getLength(); i++) {
			Element wordElement = (Element)wordNodes.item(i);
			XmlWord word = new XmlWord(wordElement);
			appendWordForms(ontologyBundle.wordSsWrapper.ontology, reasoner, factoryWrapper, document, word, formClasses);
		}
		Comment macrosComment = document.createComment("Macros");
		document.getDocumentElement().appendChild(macrosComment);
		for (OWLClass formClass : formClasses) {
			OWLSubClassOfAxiom subClassOfAxiom = reasoner.getRootOntology().getSubClassAxiomsForSubClass(formClass).iterator().next();
			OWLClass formSuperClass = (OWLClass)subClassOfAxiom.getSuperClass();
			String fragment = formClass.getIRI().getFragment();
			String superFragment = formSuperClass.getIRI().getFragment();
			Element macroElm = document.createElement("macro");
			macroElm.setAttribute("name", "@" + fragment);
			Element featureStructureElm = document.createElement("fs");
			featureStructureElm.setAttribute("attr", superFragment);
			featureStructureElm.setAttribute("val", fragment);
			featureStructureElm.setAttribute("id", "1");
			macroElm.appendChild(featureStructureElm);
			document.getDocumentElement().appendChild(macroElm);
		}
		// Save file
		CompileHelper.saveDocument(document, "CcgMorph.xml");
	}

	private static void appendWordForms(OWLOntology wordEngOntology, Reasoner reasoner, 
			DataFactoryWrapper factory, Document document, XmlWord word, Set<OWLClass> formClasses) {
		OWLOntologyManager ontologyManager = wordEngOntology.getOWLOntologyManager();
		OWLNamedIndividual oWord = factory.makeWordItem(word.id);
		OWLAxiom oWordDeclaration = factory.makeItemDeclaration(oWord);
		ontologyManager.addAxiom(wordEngOntology, oWordDeclaration);

		//OWLClass wFormClass = factory.makeWordClass("Form");
		OWLClass wCopyClass = factory.makeWordClass("Copy");
		OWLObjectProperty wHasForm = factory.makeWordProperty("hasForm");
		OWLObjectProperty wHasCopy = factory.makeWordProperty("hasCopy");

		Set<OWLClass> oWordClasses = new HashSet<OWLClass>();
		for (String wordClass : word.classSet) {

			// Classify word
			OWLClass oWordClass = factory.makeWordSsClass(wordClass);
			OWLAxiom oWordClassAssertion = factory.makeTypeAscription(oWordClass, oWord);
			ontologyManager.addAxiom(wordEngOntology, oWordClassAssertion);

			oWordClasses.add(oWordClass);
			int formIndex = 1;
			for (OWLSubClassOfAxiom subClassOf : wordEngOntology.getSubClassAxiomsForSubClass(oWordClass)) {
				OWLClassExpression superClass = subClassOf.getSuperClass();
				if (superClass instanceof OWLObjectSomeValuesFrom) {
					OWLObjectSomeValuesFrom someValuesFrom = (OWLObjectSomeValuesFrom) superClass;
					if (wHasForm.equals(someValuesFrom.getProperty())) {

						// Declare form
						OWLNamedIndividual oForm = factory.makeWordItem(word.id + "Form" + (formIndex));
						OWLAxiom oFormDeclaration = factory.makeItemDeclaration(oForm);
						ontologyManager.addAxiom(wordEngOntology, oFormDeclaration);

						// Classify form
						OWLClass formClass = (OWLClass)someValuesFrom.getFiller();
						OWLAxiom oFormClassAssertion = factory.makeTypeAscription(formClass, oForm);
						ontologyManager.addAxiom(wordEngOntology, oFormClassAssertion);

						// Ascribe form to word
						OWLAxiom oWordFormAssertion = factory.makePropertyAscription(wHasForm, oWord, oForm);
						ontologyManager.addAxiom(wordEngOntology, oWordFormAssertion);

						// Declare copy
						OWLNamedIndividual oCopy = factory.makeWordItem(word.id + "Copy" + (formIndex++));
						OWLAxiom oCopyDeclaration = factory.makeItemDeclaration(oCopy);
						ontologyManager.addAxiom(wordEngOntology, oCopyDeclaration);

						// Ascribe copy to form
						OWLAxiom oFormCopyAssertion = factory.makePropertyAscription(wHasCopy, oForm, oCopy);
						ontologyManager.addAxiom(wordEngOntology, oFormCopyAssertion);
					}
				}
			}
		}
		reasoner.flush();
		for (String sense : word.senseSet) {
			document.getDocumentElement().appendChild(document.createComment("Word " + word.id + " with sense " + sense));
			for (OWLNamedIndividual oForm : reasoner.getObjectPropertyValues(oWord, wHasForm).getFlattened()) {
				for (OWLNamedIndividual oCopy : reasoner.getObjectPropertyValues(oForm, wHasCopy).getFlattened()) {
					
					// Create word form
					CcgForm form = new CcgForm();
					form.word = word;
					form.sense = sense;

					// Classify word form
					Set<OWLClass> oFormClasses = reasoner.getTypes(oForm, false).getFlattened();
					for (OWLClass oFormClass : oFormClasses) {
						String fragment = oFormClass.getIRI().getFragment();
						if (wordEngOntology.getEquivalentClassesAxioms(oFormClass).size() == 0 && !fragment.equals("Form") && !fragment.equals("Thing")) {
							form.classes.add(fragment);
							formClasses.add(oFormClass);
						}
						if (fragment.endsWith("Form") && fragment.length() > 4) {
							form.wordClass = fragment;
						}
					}
	
					// Create word form copy
					Set<OWLClass> oCopyClasses = reasoner.getTypes(oCopy, false).getFlattened();
					oCopyClasses.retainAll(reasoner.getSubClasses(wCopyClass, true).getFlattened());
					for (OWLClass oCopyClass : oCopyClasses) {
						String fragment = oCopyClass.getIRI().getFragment();
						if (wordEngOntology.getEquivalentClassesAxioms(oCopyClass).size() == 0 && fragment.split("_").length == 1) {
							form.copy = makeCopy(word, fragment);
						}
					}
					appendEntry(document, form);
				}

			}
		}
	}

	private static String makeCopy(XmlWord word, String fragment) {
		String copy = "xxxxx";
//		System.out.println(fragment);
		String[] commands = fragment.split("[+]");
		for (String command : commands) {
			if (command.equals("Stem")) {
				copy = word.sampleMap.get("stem");
			}
			if (command.equals("Stem01")) {
				copy = word.sampleMap.get("stem01");
			}
			if (command.equals("Stem02")) {
				copy = word.sampleMap.get("stem02");
			}
			if (command.equals("Stem03")) {
				copy = word.sampleMap.get("stem03");
			}
			if (command.equals("Stem04")) {
				copy = word.sampleMap.get("stem04");
			}
			if (command.equals("Stem05")) {
				copy = word.sampleMap.get("stem05");
			}
			if (command.equals("Stem06")) {
				copy = word.sampleMap.get("stem06");
			}
			if (command.equals("Stem07")) {
				copy = word.sampleMap.get("stem07");
			}
			if (command.equals("Stem08")) {
				copy = word.sampleMap.get("stem08");
			}
			if (command.equals("Chop")) {
				copy = copy.substring(0, copy.length() - 1);
			}
			if (command.equals("ChangeYtoI")) {
				copy = copy.substring(0, copy.length() - 1) + "i";
			}
			if (command.equals("Double")) {
				copy += copy.charAt(copy.length() - 1);
			}
			if (command.equals("SuffixS")) {
				copy += "s";
			}
			if (command.equals("SuffixS'")) {
				copy += "s'";
			}
			if (command.equals("Suffix'S")) {
				copy += "'s";
			}
			if (command.equals("SuffixEs")) {
				copy += "es";
			}
			if (command.equals("SuffixD")) {
				copy += "d";
			}
			if (command.equals("SuffixEd")) {
				copy += "ed";
			}
			if (command.equals("SuffixEr")) {
				copy += "er";
			}
			if (command.equals("SuffixR")) {
				copy += "r";
			}
			if (command.equals("SuffixEst")) {
				copy += "est";
			}
			if (command.equals("SuffixSt")) {
				copy += "st";
			}
			if (command.equals("SuffixIng")) {
				copy += "ing";
			}
			if (command.equals("PrefixMore")) {
				copy = "more-" + copy;
			}
			if (command.equals("PrefixMost")) {
				copy = "most-" + copy;
			}
//			System.out.println("- " + copy);
		}
		return copy;
	}

	private static void appendEntry(Document document, CcgForm form) {
		Element entry = makeEntry(document, form.word.id, form.wordClass,
				form.sense, form.copy, form.classes);
		document.getDocumentElement().appendChild(entry);
	}

	private static Element makeEntry(Document document, String wordId,
			String wordClass, String sense, String copy,
			Set<String> wordFormClassNames) {
		Element entry = document.createElement("entry");
		entry.setAttribute("stem", wordId);
		entry.setAttribute("pos", wordClass);
		entry.setAttribute("class", sense);
		entry.setAttribute("word", copy);
		String macros = "";
		for (String wordFormClassName : wordFormClassNames) {
			macros += "@" + wordFormClassName + " ";
		}
		entry.setAttribute("macros", macros.trim());
		return entry;
	}

}
