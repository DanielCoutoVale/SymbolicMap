package org.uppermodel.tools;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Creates the word forms and store them in CcgMorph.xml file for OpenCCG.
 * 
 * @author Daniel Couto Vale <danielvale@uni-bremen.de>
 */
public class CompileIntoCcgLexiconCcgRules {

	public final static void main(String[] args) throws OWLOntologyCreationException, URISyntaxException, ParserConfigurationException, TransformerConfigurationException, IOException, TransformerFactoryConfigurationError, TransformerException {
		OntologyBundle ontologyBundle = CompileHelper.makeOntologyBundle("Eng");
		Reasoner reasoner = new Reasoner(ontologyBundle.wordingSsWrapper.ontology);
		DataFactoryWrapper factory = new DataFactoryWrapper(reasoner.getDataFactory(), "Eng");

		OWLClass patternClass = factory.makeWordingClass("WordingAsPattern");
		Set<OWLClass> patternSubClasses = reasoner.getSubClasses(patternClass, true).getFlattened();
		Document rulDocument = makeRulDocument();
		Document lexDocument = makeLexDocument();

		makeIncompletes(reasoner, factory, lexDocument, rulDocument, patternSubClasses);

		CompileHelper.saveDocument(rulDocument, "CcgRules.xml");
		CompileHelper.saveDocument(lexDocument, "CcgLexicon.xml");
	}

	private final static void makeIncompletes(Reasoner reasoner, DataFactoryWrapper factory, Document lexDocument, Document rulDocument, Set<OWLClass> oClasses) {
		for (OWLClass oClass : oClasses) {
			if (oClass.isDefined(reasoner.getRootOntology())) {
				makeIncomplete(reasoner, factory, lexDocument, rulDocument, oClass);
			}
		}
	}

	private final static void makeIncomplete(Reasoner reasoner, DataFactoryWrapper factory, Document lexDocument, Document rulDocument, OWLClass oClass) {
		Set<OWLClass> oSubClasses = reasoner.getSubClasses(oClass, true).getFlattened();
		oSubClasses.remove(reasoner.getDataFactory().getOWLNothing());
		makeIncompletes(reasoner, factory, lexDocument, rulDocument, oSubClasses);
		if (oSubClasses.size() == 0) {
			Set<OWLClass> oSuperClasses = reasoner.getSuperClasses(oClass, false).getFlattened();
			OWLClass oCategory = makeIncompleteCategory(reasoner, oSuperClasses);
			if (oCategory == null) {
				return;
			}
			String oClassFragment = oClass.getIRI().getFragment();
			String oCategoryFragment = oCategory.getIRI().getFragment();
			System.out.println("-" + oClassFragment);
			if (oCategoryFragment.contains("=")) {
				// Grammatical Atom
				makeAtom(reasoner, factory, rulDocument, oSuperClasses, oCategoryFragment, oClassFragment, oClass);
			} else {
				// Grammatical Head + Grammatical Tail
				makeHeadTail(reasoner, factory, lexDocument, oSuperClasses, oCategoryFragment, oClassFragment, oClass);
			}
		}
	}

	private final static void makeHeadTail(Reasoner reasoner, DataFactoryWrapper factory, Document lexDocument, Set<OWLClass> oSuperClasses, String oCategoryFragment, String oClassFragment, OWLClass oClass) {
		
		// Entry
		Symbol symbol = makeSymbol(oCategoryFragment);
		Element entryElm = lexDocument.createElement("entry");
		Element entrySymbolElm = makeSymbolElm(lexDocument, symbol, new IndexHolder());
		entryElm.appendChild(entrySymbolElm);
		appendHeadFeature(reasoner, factory, lexDocument, oClass, entrySymbolElm);

		Set<String> fragmentSet = selectMatchingFragments(oSuperClasses, "lexify_[A-Za-z-]*_([A-Za-z-]*)");
		String headWord = fragmentSet.iterator().next(); 

		// Family
		Element familyElm = lexDocument.createElement("family");
		familyElm.setAttribute("name", oClassFragment);
		familyElm.setAttribute("closed", "false");
		familyElm.setAttribute("pos", headWord);
		familyElm.appendChild(entryElm);
		
		// Logical Form
		appendLogicalForm(reasoner, factory, lexDocument, oSuperClasses, entrySymbolElm, familyElm);
	}

	private final static void makeAtom(Reasoner reasoner, DataFactoryWrapper factory, Document rulDocument, Set<OWLClass> oSuperClasses, String oCategoryFragment, String oClassFragment, OWLClass oClass) {
		IndexHolder indexHolder = new IndexHolder();

		// Source
		String source = oCategoryFragment.split("=")[0];
		Symbol sourceSymbol = makeSymbol(source);
		Element sourceElm = rulDocument.createElement("arg");
		Element sourceSymbolElm = makeSymbolElm(rulDocument, sourceSymbol, indexHolder);
		sourceElm.appendChild(sourceSymbolElm);

		// Target
		String target = oCategoryFragment.split("=")[1];
		Symbol targetSymbol = makeSymbol(target);
		Element targetElm = rulDocument.createElement("result");
		Element targetSymbolElm = makeSymbolElm(rulDocument, targetSymbol, indexHolder);
		targetElm.appendChild(targetSymbolElm);
		appendHeadFeature(reasoner, factory, rulDocument, oClass, targetSymbolElm);
		

		// Change
		Element categoryChangeElm = rulDocument.createElement("typechanging");
		categoryChangeElm.setAttribute("name", oClassFragment);
		categoryChangeElm.appendChild(sourceElm);
		categoryChangeElm.appendChild(targetElm);

		// Logical Form
		appendLogicalForm(reasoner, factory, rulDocument, oSuperClasses, targetSymbolElm, categoryChangeElm);
	}

	private final static void appendHeadFeature(Reasoner reasoner, DataFactoryWrapper factory, Document document, OWLClass oClass, Element symbolElm) {
		Set<OWLEquivalentClassesAxiom> oEquivalents = reasoner.getRootOntology().getEquivalentClassesAxioms(oClass);
		for (OWLEquivalentClassesAxiom oEquivalent : oEquivalents) {
			Set<OWLClassExpression> oPatternClasses = oEquivalent.getClassExpressionsMinus(oClass); 
			Set<OWLClass> oBrickClasses = extractBrickClasses(reasoner, factory, oPatternClasses);
			for (OWLClass oBrickClass : oBrickClasses) {
				if (reasoner.getRootOntology().getEquivalentClassesAxioms(oBrickClass).size() == 0) {
					Element featureElm = makeFeatureElm(reasoner, document, oBrickClass);
					if (symbolElm.getNodeName().equals("complexcat")) {
						symbolElm
							.getChildNodes().item(0)
							.getChildNodes().item(0)
							.appendChild(featureElm);
					} else {
						symbolElm
							.getChildNodes().item(0)
							.appendChild(featureElm);
					}
				}
			}
		}
	}

	private static Element makeFeatureElm(Reasoner reasoner, Document document, OWLClass oBrickClass) {
		OWLSubClassOfAxiom subClassOf = reasoner.getRootOntology().getSubClassAxiomsForSubClass(oBrickClass).iterator().next();
		OWLClass oBrickSuperClass = (OWLClass)subClassOf.getSuperClass();
		String superFragment = oBrickSuperClass.getIRI().getFragment();
		String fragment = oBrickClass.getIRI().getFragment();
		Element featureElm = document.createElement("feat");
		featureElm.setAttribute("attr", superFragment);
		featureElm.setAttribute("val", fragment);
		return featureElm;
	}

	private static Set<OWLClass> extractBrickClasses(Reasoner reasoner, DataFactoryWrapper factory, Set<OWLClassExpression> oPatternClasses) {
		OWLClass oBrickClass = factory.makeWordingClass("WordingAsBrick");
		Set<OWLClass> oBrickClasses = new HashSet<OWLClass>();
		for (OWLClassExpression oPatternClass : oPatternClasses) {
			for (OWLClass oClass : oPatternClass.getClassesInSignature()) {
				OWLAxiom oAxiom = factory.makeSubClassOf(oClass, oBrickClass);
				reasoner.flush();
				if (reasoner.isEntailed(oAxiom)) {
					oBrickClasses.add(oClass);
				}
			}
		}
		return oBrickClasses;
	}

	private final static void appendLogicalForm(Reasoner reasoner, DataFactoryWrapper factory, Document document, Set<OWLClass> oSuperClasses, Element entrySymbolElm, Element entryHolderElm) {
		Element logicalFormElm = document.createElement("lf");
		Element saturatedOperatorElm = document.createElement("satop");
		saturatedOperatorElm.setAttribute("nomvar", "Onus");
		NodeList nodeList = entryHolderElm.getElementsByTagName("nomvar");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element nomvarElm = (Element)nodeList.item(i);
			String variable = nomvarElm.getAttribute("name");
			if (variable.contains(".")) {
				nomvarElm.setAttribute("name", variable.split("[.]")[0]);
				variable = variable.split("[.]")[1];
			}
			makeIdentify(document, oSuperClasses, saturatedOperatorElm, variable);
			makePreselectDevice(reasoner, factory, document, oSuperClasses, nomvarElm, variable);
			makePreselectBrick(reasoner, factory, document, oSuperClasses, nomvarElm, variable);
			makeAgreeBrick(reasoner, factory, document, oSuperClasses, nomvarElm, variable);
		}
		if (saturatedOperatorElm.getChildNodes().getLength() > 0) {
			logicalFormElm.appendChild(saturatedOperatorElm);
			entrySymbolElm.appendChild(logicalFormElm);
		}
		document.getDocumentElement().appendChild(entryHolderElm);
	}

	private static void makePreselectDevice(Reasoner reasoner, DataFactoryWrapper factory, Document rulDocument, Set<OWLClass> oSuperClasses, Element nomvarElm, String variable) {
		Set<String> fragmentSet = selectMatchingFragments(oSuperClasses, "preselect_" + variable + "_([A-Za-z0-9-]*)");
		OWLClass deviceClass = factory.makeWordingClass("WordingAsDevice");
		for (String fragment : fragmentSet) {
			OWLClass fragmentClass = factory.makeWordingSsClass(fragment);
			OWLAxiom subClassOf = factory.makeSubClassOf(fragmentClass, deviceClass);
			if (reasoner.isEntailed(subClassOf)) {
				nomvarElm.setAttribute("name", nomvarElm.getAttribute("name") + ":" + fragment);
			}
		}
	}

	private static void makeAgreeBrick(Reasoner reasoner, DataFactoryWrapper factory, Document rulDocument, Set<OWLClass> oSuperClasses, Element nomvarElm, String variable) {
		Set<String> fragmentSet = selectMatchingFragments(oSuperClasses, "agree_[A-Za-z0-9-]*_" + variable + "_([A-Za-z0-9-]*)");
		fragmentSet.addAll(selectMatchingFragments(oSuperClasses, "agree_" + variable + "_[A-Za-z0-9-]*_([A-Za-z0-9-]*)"));
		OWLClass brickClass = factory.makeWordingClass("WordingAsBrick");
		for (String fragment : fragmentSet) {
			OWLClass fragmentClass = factory.makeWordingSsClass(fragment);
			OWLAxiom subClassOf = factory.makeSubClassOf(fragmentClass, brickClass);
			if (reasoner.isEntailed(subClassOf)) {
				Element featureStructureElm = (Element)nomvarElm.getParentNode().getParentNode().getParentNode();
				Element feature = rulDocument.createElement("feat");
				featureStructureElm.appendChild(feature);
				feature.setAttribute("attr", fragment);
				Element featureVariable = rulDocument.createElement("featvar");
				featureVariable.setAttribute("name", fragment);
				feature.appendChild(featureVariable);
			}
		}
	}

	private static void makePreselectBrick(Reasoner reasoner, DataFactoryWrapper factory, Document rulDocument, Set<OWLClass> oSuperClasses, Element nomvarElm, String variable) {
		Set<String> fragmentSet = selectMatchingFragments(oSuperClasses, "preselect_" + variable + "_([A-Za-z0-9-]*)");
		OWLClass brickClass = factory.makeWordingClass("WordingAsBrick");
		for (String fragment : fragmentSet) {
			OWLClass fragmentClass = factory.makeWordingSsClass(fragment);
			OWLAxiom subClassOf = factory.makeSubClassOf(fragmentClass, brickClass);
			if (reasoner.isEntailed(subClassOf)) {
				Element featureStructureElm = (Element)nomvarElm.getParentNode().getParentNode().getParentNode();
				Element featureElm = makeFeatureElm(reasoner, rulDocument, fragmentClass);
				featureStructureElm.appendChild(featureElm);
			}
		}
	}

	private static void makeIdentify(Document rulDocument, Set<OWLClass> oSuperClasses, Element saturatedOperatorElm, String variable) {
		Set<String> framgentSet = selectMatchingFragments(oSuperClasses, "identify_" + variable + "_[(]([A-Za-z0-9-]*)Id_Onus[)]");
		for (String fragment : framgentSet) {
			Element diamondElm = rulDocument.createElement("diamond");
			diamondElm.setAttribute("mode", "has" + fragment);
			Element nomvarElm = rulDocument.createElement("nomvar");
			nomvarElm.setAttribute("name", variable);
			diamondElm.appendChild(nomvarElm);
			saturatedOperatorElm.appendChild(diamondElm);
		}
	}

	private static Set<String> selectMatchingFragments(Set<OWLClass> oClasses, String regex) {
		Set<String> oSelectedFragments = new HashSet<String>();
		for (OWLClass oClass : oClasses) {
			String fragment = oClass.getIRI().getFragment();
			if (fragment.matches(regex)) {
				oSelectedFragments.add(fragment.replaceAll(regex, "$1"));
			}
			
		}
		return oSelectedFragments;
	}

	private static class IndexHolder {
		int index = 1;
	} 
	
	private static Element makeSymbolElm(Document document, Symbol symbol, IndexHolder indexHolder) {
		if (symbol instanceof CompleteSymbol) {
			CompleteSymbol completeSymbol = (CompleteSymbol) symbol;
			Element symbolElm = makeCompleteSymbolElm(document, completeSymbol.category, completeSymbol.variable, indexHolder);
			return symbolElm;
		}
		if (symbol instanceof IncompleteSymbol) {
			IncompleteSymbol incompleteSymbol = (IncompleteSymbol) symbol;
			Element symbolElm = document.createElement("complexcat");
			symbolElm.appendChild(makeCompleteSymbolElm(document, incompleteSymbol.whole.category, incompleteSymbol.whole.variable, indexHolder));
			for (Parameter parameter : incompleteSymbol.parameters) {
				Element slashElm = document.createElement("slash");
				if (parameter.slash.equals("BP")) {
					slashElm.setAttribute("dir", "\\");
				} else {
					slashElm.setAttribute("dir", "/");
				}
				slashElm.setAttribute("mode", ".");
				symbolElm.appendChild(slashElm);
				symbolElm.appendChild(makeSymbolElm(document, parameter.symbol, indexHolder));
			}
			return symbolElm;
		}
		return document.createElement("error");
	}

	private static Symbol makeSymbol(String linearForm) {
		if (linearForm.contains("'FP'") || linearForm.contains("'BP'")) {
			IncompleteSymbol symbol = new IncompleteSymbol();
			int index = nextSlashIndex(linearForm);
			symbol.whole = (CompleteSymbol)makeSymbol(linearForm.substring(0, index));
			while (index != -1) {
				Parameter parameter = new Parameter();
				parameter.slash = linearForm.substring(index + 1, index + 3);
				linearForm = linearForm.substring(index + 4);
				index = nextSlashIndex(linearForm);
				String sublinearForm = linearForm;
				if (index > -1) {
					sublinearForm = linearForm.substring(0, index); 
				}
				parameter.symbol = makeSymbol(sublinearForm);
				symbol.parameters.add(parameter);
			}
			return symbol;
		}
		CompleteSymbol symbol = new CompleteSymbol();
		symbol.category = linearForm.split(":")[0];
		symbol.variable = linearForm.split(":")[1];
		return symbol;
	}

	private static int nextSlashIndex(String linearForm) {
		int fpIndex = linearForm.indexOf("'FP'");
		int bpIndex = linearForm.indexOf("'BP'");
		int index = -1;
		if (fpIndex > 0 && (bpIndex == -1 || fpIndex < bpIndex)) {
			index = fpIndex;
		} else {
			index = bpIndex;
		}
		return index;
	}

	public static interface Symbol {}
	public static class CompleteSymbol implements Symbol {
		String category;
		String variable;
	}
	public static class IncompleteSymbol implements Symbol {
		CompleteSymbol whole;
		List<Parameter> parameters = new LinkedList<Parameter>();
	}
	public static class Parameter {
		String slash;
		String mode;
		Symbol symbol;
	}

	private final static OWLClass makeIncompleteCategory(Reasoner reasoner, Set<OWLClass> oSuperClasses) {
		for (OWLClass oSuperClass : oSuperClasses) {
			String fragment = oSuperClass.getIRI().getFragment();
			if (!oSuperClass.isDefined(reasoner.getRootOntology()) && fragment.contains(":")) {
				return oSuperClass;
			}
		}
		return null;
	}

	private final static Document makeRulDocument() throws ParserConfigurationException {
		Document rulDocument = CompileHelper.makeDocument();
		Element rules = rulDocument.createElement("rules");
		rules.setAttribute("name", "English");
		rules.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		rules.setAttribute("xsi:noNamespaceSchemaLocation", "../rules.xsd");
		rulDocument.appendChild(rules);
		appendRule(rulDocument, "application", "forward", null, null);
		appendRule(rulDocument, "application", "backward", null, null);
		appendRule(rulDocument, "composition", "forward", "true", null);
		appendRule(rulDocument, "composition", "forward", "false", null);
		appendRule(rulDocument, "composition", "backward", "true", null);
		appendRule(rulDocument, "composition", "backward", "false", null);
		appendRule(rulDocument, "typeraising", "forward", null, "true");
		appendRule(rulDocument, "typeraising", "backward", null, "true");
		appendRule(rulDocument, "substitution", "forward", "true", null);
		appendRule(rulDocument, "substitution", "forward", "false", null);
		appendRule(rulDocument, "substitution", "backward", "true", null);
		appendRule(rulDocument, "substitution", "backward", "false", null);
		return rulDocument;
	}

	private static void appendRule(Document rulDocument, String ruleName,
			String direction, String harmonic, String useDollar) {
		Element elm = rulDocument.createElement(ruleName);
		elm.setAttribute("dir", direction);
		if (harmonic != null) {
			elm.setAttribute("harmonic", harmonic);
		}
		if (useDollar != null) {
			elm.setAttribute("useDollar", useDollar);
		}
		rulDocument.getDocumentElement().appendChild(elm);
	}

	private final static Document makeLexDocument() throws ParserConfigurationException {
		Document lexDocument = CompileHelper.makeDocument();
		Element lexElm = lexDocument.createElement("ccg-lexicon");
		lexElm.setAttribute("name", "English");
		lexElm.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		lexElm.setAttribute("xsi:noNamespaceSchemaLocation", "../lexicon.xsd");
		lexDocument.appendChild(lexElm);
		makeLexicalRecognition(lexDocument, "ProcessForm",    "Process",    "Process", new IndexHolder());
		makeLexicalRecognition(lexDocument, "MentionerForm",  "Mention",    "SThing", new IndexHolder());
		makeLexicalRecognition(lexDocument, "ClassifierForm", "Classifier", "SThing", new IndexHolder());
		makeLexicalRecognition(lexDocument, "IdentifierForm", "Identifier", "Name", new IndexHolder());
		makeLexicalRecognition(lexDocument, "QualifierForm",  "Qualifier",  "Quality", new IndexHolder());
		return lexDocument;
	}

	private final static void makeLexicalRecognition(Document lexDocument, String wordClass, String wordingClass, String phenomenonClass, IndexHolder indexHolder) {
		Element familyElm = lexDocument.createElement("family");
		familyElm.setAttribute("closed", "false");
		familyElm.setAttribute("pos", wordClass);
		familyElm.setAttribute("name", wordClass);
		Element entryElm = lexDocument.createElement("entry");
		entryElm.setAttribute("name", "LexicalWord");

		Element atomElm = makeAtomicCategoryElm(lexDocument, wordingClass, "Current", phenomenonClass, indexHolder);
		
		entryElm.appendChild(atomElm);
		familyElm.appendChild(entryElm);
		lexDocument.getDocumentElement().appendChild(familyElm);
	}

	private final static Element makeAtomicCategoryElm(Document document, String wordingClass, String variable, String phenomenonClass, IndexHolder indexHolder) {
		Element logicalFormElm;
		Element atomElm = makeCompleteSymbolElm(document, wordingClass, variable, indexHolder);
		// Semantic Layer
		logicalFormElm = document.createElement("lf");
		Element saturatedOperatorElm = document.createElement("satop");
		saturatedOperatorElm.setAttribute("nomvar", variable + ":" + phenomenonClass);
		Element property = document.createElement("prop");
		property.setAttribute("name", "[*DEFAULT*]");
		saturatedOperatorElm.appendChild(property);
		logicalFormElm.appendChild(saturatedOperatorElm);
		atomElm.appendChild(logicalFormElm);
		return atomElm;
	}

	private final static Element makeCompleteSymbolElm(Document document, String wordingClass, String variable, IndexHolder indexHolder) {
		Element atomElm = document.createElement("atomcat");
		atomElm.setAttribute("type", wordingClass);
		// Symbolic Layer
		Element featureStructureElm = document.createElement("fs");
		featureStructureElm.setAttribute("id", "" + (indexHolder.index++));
		Element featureElm = document.createElement("feat");
		featureElm.setAttribute("attr", "index");
		Element logicalFormElm = document.createElement("lf");
		Element nominalVariableElm = document.createElement("nomvar");
		nominalVariableElm.setAttribute("name", variable);
		logicalFormElm.appendChild(nominalVariableElm);
		featureElm.appendChild(logicalFormElm);
		featureStructureElm.appendChild(featureElm);
		atomElm.appendChild(featureStructureElm);
		return atomElm;
	}

}
