package org.uppermodel.tools;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.kpml.resource.AssociationValuesSlot;
import org.kpml.resource.Decision;
import org.kpml.resource.Feature;
import org.kpml.resource.xml.XDecision;
import org.kpml.resource.xml.XFeature;
import org.kpml.resource.xml.XFeatureSelector;
import org.kpml.resource.xml.XFeatureSystem;
import org.kpml.resource.xml.XGate;
import org.kpml.resource.xml.XInquiry;
import org.kpml.resource.xml.XInquiryCall;
import org.kpml.resource.xml.XModule;
import org.kpml.resource.xml.XPattern;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.w3c.dom.Document;

/**
 * Generates the systemic network from a description
 * 
 * @author Daniel Couto Vale <danielvale@uni-bremen.de>
 */
public class CompileIntoKpmlNetwork {

	public static class Factory {
		private final OWLDataFactory dataFactory;

		public Factory(Reasoner reasoner) {
			this.dataFactory = reasoner.getDataFactory();
		}

		private final OWLClass makeWordClass(String token) {
			return dataFactory.getOWLClass(IRI.create("http://www.uppermodel.org/Word" + token));
		} 

		public final OWLClass getWordClass() {
			return makeWordClass("#Word");
		}

		public OWLClass getFormClass() {
			return makeWordClass("#Form");
		}

		public OWLClass getCopyClass() {
			return makeWordClass("#Copy");
		}

		private final OWLClass makeWordingClass(String token) {
			return dataFactory.getOWLClass(IRI.create("http://www.uppermodel.org/Wording" + token));
		} 

		public final OWLClass getWordingAsDeviceClass() {
			return makeWordingClass("#WordingAsDevice");
		}

		public OWLClass getWordingAsBrickClass() {
			return makeWordingClass("#WordingAsBrick");
		}

		public OWLClass getWordingAsPatternClass() {
			return makeWordingClass("#WordingAsPattern");
		}

		public OWLAxiom getDisjoint(Set<OWLClass> classSet, OWLClass newClass) {
			Set<OWLClass> newClassSet = new HashSet<OWLClass>();
			newClassSet.addAll(classSet);
			newClassSet.add(newClass);
			return dataFactory.getOWLDisjointClassesAxiom(newClassSet);
		}
	}
	

	/**
	 * @param args
	 * @throws OWLOntologyCreationException
	 * @throws URISyntaxException 
	 * @throws ParserConfigurationException 
	 * @throws TransformerException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws OWLOntologyCreationException, URISyntaxException, ParserConfigurationException, TransformerException, IOException {

		// Create IRIs
		OntologyBundle bundle = CompileHelper.makeOntologyBundle("Eng");
		Reasoner wordReasoner = new Reasoner(bundle.wordSsWrapper.ontology);
		wordReasoner.flush();
		Factory factory = new Factory(wordReasoner);

		// Get main components
		XModule xModule = new XModule();
		
		// Systematize word types
		systematizeDeviceTypes(xModule, bundle.wordSsWrapper, factory.getWordClass());

		// Systematize form types
		makeBrickGate(xModule, bundle.wordWrapper, factory.getFormClass());
		systematizeBrickTypes(xModule, bundle.wordSsWrapper, factory.getFormClass());

		// Derive copy gates
		makePatternGate(xModule, bundle.wordWrapper, factory.getCopyClass());
		systematizePatternTypes(xModule, bundle.wordSsWrapper, factory.getCopyClass());

		// Save file
		Document document = xModule.getDocument();
		CompileHelper.saveDocument(document, "KpmlWord.xml");
		
		// Create IRIs
		XModule xModuleWording = new XModule();
		Reasoner wordingReasoner = new Reasoner(bundle.wordingSsWrapper.ontology);
		wordingReasoner.flush();
		Factory wordingFactory = new Factory(wordingReasoner);

		// Systematize wordingAsDevice types
		systematizeDeviceTypes(xModuleWording, bundle.wordingSsWrapper, wordingFactory.getWordingAsDeviceClass());

		// Systematize wordingAsBrick types
		makeBrickGate(xModuleWording, bundle.wordingWrapper, wordingFactory.getWordingAsBrickClass());
		systematizeBrickTypes(xModuleWording, bundle.wordingSsWrapper, wordingFactory.getWordingAsBrickClass());

		// Derive wordingAsPattern gates
		makePatternGate(xModuleWording, bundle.wordingWrapper, wordingFactory.getWordingAsPatternClass());
		systematizePatternTypes(xModuleWording, bundle.wordingSsWrapper, wordingFactory.getWordingAsPatternClass());

		// Save file
		Document wordingDocument = xModuleWording.getDocument();
		CompileHelper.saveDocument(wordingDocument, "KpmlWording.xml");
	}

	/**
	 * Make a form gate
	 * 
	 * @param xModule the x-module
	 * @param ontology the ontology
	 * @param gate the form class
	 */
	private final static void makeBrickGate(XModule xModule, OntologyWrapper ontologyWrapper, OWLClass formClass) {
		Set<OWLEquivalentClassesAxiom> axiomSet = ontologyWrapper.ontology.getEquivalentClassesAxioms(formClass);
		for (OWLEquivalentClassesAxiom axiom : axiomSet) {
			OWLClassExpression oClassExp = axiom.getClassExpressionsAsList().get(1);
			Set<OWLClass> oClassSet = new HashSet<OWLClass>();
			oClassSet.add(formClass);
			makeFormGate(xModule, oClassExp, oClassSet);
		}
	}

	/**
	 * Make a copy gate
	 * 
	 * @param xModule the x-module
	 * @param ontology the ontology
	 * @param gateClass the copy class
	 */
	private final static void makePatternGate(XModule xModule, OntologyWrapper ontologyWrapper, OWLClass gateClass) {
		Set<OWLEquivalentClassesAxiom> axiomSet = ontologyWrapper.ontology.getEquivalentClassesAxioms(gateClass);
		for (OWLEquivalentClassesAxiom axiom : axiomSet) {
			OWLClassExpression oClassExp = axiom.getClassExpressionsAsList().get(1);
			makeCopyGate(xModule, ontologyWrapper, oClassExp, gateClass);
		}
	}

	/**
	 * Systematize copy types
	 * 
	 * @param xModule the x-module
	 * @param ontologyWrapper the ontology
	 * @param copyClass the copy class
	 */
	private final static void systematizePatternTypes(XModule xModule, OntologyWrapper ontologyWrapper, OWLClass copyClass) {
		Set<OWLClass> copySubClasses = ontologyWrapper.fetchSubClasses(copyClass);
		Set<OWLClass> copyGates = ontologyWrapper.selectDefinedClasses(copySubClasses);
		for (OWLClass copyGate : copyGates) {
			makePatternGate(xModule, ontologyWrapper, copyGate);
			systematizePatternTypes(xModule, ontologyWrapper, copyGate);
		}
	}

	/**
	 * Systematize form types
	 * 
	 * @param xModule the x-module
	 * @param ontologyWrapper the ontology
	 * @param formClass the form class
	 */
	private static void systematizeBrickTypes(XModule xModule, OntologyWrapper ontologyWrapper, OWLClass formClass) {
		Set<OWLClass> formSubClasses = ontologyWrapper.fetchSubClasses(formClass);
		Set<Set<OWLClass>> formSystems = ontologyWrapper.groupDisjunctClasses(formSubClasses);
		for (Set<OWLClass> formSystem : formSystems) {
			Set<OWLClass> aFormSystem = ontologyWrapper.selectUndefinedClasses(formSystem);
			Set<OWLClass> bFormGates = ontologyWrapper.selectDefinedClasses(formSystem);
			if (aFormSystem.size() > 0) {
				makeFormTypeSystem(xModule, formClass, formSystem);
			}
			for (OWLClass formGate : bFormGates) {
				makeBrickGate(xModule, ontologyWrapper, formGate);
			}
			for (OWLClass formSubClass : formSystem) {
				systematizeBrickTypes(xModule, ontologyWrapper, formSubClass);
			}
		}
	}

	/**
	 * Systematize word types
	 * 
	 * @param xModule the x-module
	 * @param ontologyWrapper the ontology
	 * @param wordClass the word class
	 */
	private final static void systematizeDeviceTypes(XModule xModule, OntologyWrapper ontologyWrapper, OWLClass wordClass) {
		for (Set<OWLClass> wordSystem : ontologyWrapper.fetchDisjointUnionsByDefinition(wordClass, false)) {
			makeWordTypeSystem(xModule, wordClass, wordSystem);
			for (OWLClass wordSubClass : wordSystem) {
				systematizeDeviceTypes(xModule, ontologyWrapper, wordSubClass);
			}
		}
	}

	private final static XGate makeGate(Document document, OWLClassExpression expression) {
		if (expression instanceof OWLObjectSomeValuesFrom) {
			OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom)expression;
			return makeGate(document, some.getFiller());
		}
		XGate xGate = new XGate(document);
		if (expression instanceof OWLClass) {
			OWLClass klass = (OWLClass)expression;
			xGate.beFeature(klass.getIRI().getFragment());
		} else if (expression instanceof OWLObjectIntersectionOf) {
			OWLObjectIntersectionOf intersection = (OWLObjectIntersectionOf)expression;
			xGate.beAnd();
			Set<OWLClassExpression> operandSet = intersection.getOperands();
			for (OWLClassExpression operand : operandSet) {
				xGate.addParam(makeGate(document, operand));
			}
		} else if (expression instanceof OWLObjectUnionOf) {
			OWLObjectUnionOf union = (OWLObjectUnionOf)expression;
			xGate.beOr();
			Set<OWLClassExpression> operandSet = union.getOperands();
			for (OWLClassExpression operand : operandSet) {
				xGate.addParam(makeGate(document, operand));
			}
		} else if (expression instanceof OWLObjectComplementOf) {
			OWLObjectComplementOf complement = (OWLObjectComplementOf)expression;
			xGate.beNot();
			OWLClassExpression operand = complement.getObjectComplementOf();
			xGate.addParam(makeGate(document, operand));
		}
		return xGate;
	}

	private static int wordTypeIndex = 1;
	private static final void makeWordTypeSystem(XModule xModule, OWLClassExpression oClassExp, Set<OWLClass> oClassSet) {
		XFeatureSystem xSystem = makeSystem(xModule, oClassExp, oClassSet, "WORD-TYPE-" + (wordTypeIndex++));
		xSystem.setSelectorIndex(xSystem.getId() + "-SELECTOR");
		makeWordTypeSelector(xModule, oClassSet, xSystem.getSelectorIndex());
	}

	private static int formTypeIndex = 1;
	private static final void makeFormTypeSystem(XModule xModule, OWLClassExpression oClassExp, Set<OWLClass> oClassSet) {
		XFeatureSystem xSystem = makeSystem(xModule, oClassExp, oClassSet, "FORM-TYPE-" + (formTypeIndex++));
		xSystem.setSelectorIndex(xSystem.getId() + "-SELECTOR");
		makeFormTypeSelector(xModule, oClassSet, xSystem.getSelectorIndex());
	}

	private static int formGateIndex = 1;
	private static final void makeFormGate(XModule xModule, OWLClassExpression oClassExp, Set<OWLClass> oClassSet) {
		makeSystem(xModule, oClassExp, oClassSet, "COPY-GATE-" + (formGateIndex++));
	}

	private static int copyGateIndex = 1;
	private static final void makeCopyGate(XModule xModule, OntologyWrapper ontologyWrapper, OWLClassExpression oClassExp, OWLClass oClass) {
		Set<OWLClass> oClassSet = new HashSet<OWLClass>();
		oClassSet.add(oClass);
		XFeatureSystem xSystem = makeSystem(xModule, oClassExp, oClassSet, "COPY-GATE-" + (copyGateIndex));
		Feature feature = xSystem.getOutputs().get(0);
		makePatterns(xModule, feature, ontologyWrapper, oClass);
	}

	private static final XFeatureSystem makeSystem(XModule xModule, OWLClassExpression inputClassExp, Set<OWLClass> outputClassSet, String id) {
		XFeatureSystem xSystem = new XFeatureSystem(xModule.getDocument());
		xSystem.setId(id);
		for (OWLClass klass : outputClassSet) {
			XFeature xFeature = new XFeature(xModule.getDocument(), xSystem);
			xFeature.setId(klass.getIRI().getFragment());
			xFeature.setShare(Double.toString(1.0 / outputClassSet.size()));
			xSystem.addOutput(xFeature);
		}
		XGate xGate = makeGate(xModule.getDocument(), inputClassExp);
		xSystem.setGate(xGate);
		xModule.getDocument().getDocumentElement().appendChild(xSystem.getElement());
		return xSystem;
	}

	private final static void makePatterns(XModule xModule, Feature feature, OntologyWrapper ontologyWrapper, OWLClass oClass) {
		Set<OWLClass> oSuperClasses = ontologyWrapper.fetchSuperClasses(oClass);
		for (OWLClass oPattern : ontologyWrapper.selectUndefinedClasses(oSuperClasses)) {
			String fragment = oPattern.getIRI().getFragment();
			String[] fragmentParts = fragment.split("_");
			if (fragmentParts.length == 1) {
				continue;
			}
			XPattern xPattern = new XPattern(xModule.getDocument());
			xPattern.setType(fragmentParts[0]);
			xPattern.setParam1(fragmentParts[1]);
			if (fragmentParts.length > 2) {
				xPattern.setParam2(fragmentParts[2]);
			}
			feature.addPattern(xPattern);
		}
	}

	private static final XFeatureSelector makeWordTypeSelector(XModule xModule, Set<OWLClass> answerClassSet, String id) {
		XFeatureSelector xSelector = new XFeatureSelector(xModule.getDocument());
		xSelector.setId(id);
		XInquiryCall xInquiryCall = makeInquiryCall(xModule, id);
		XDecision xAsk = makeAsk(xModule, xInquiryCall, answerClassSet);
		XInquiry xInquiry = makeInquiry(xModule, answerClassSet, id);
		xModule.addPiece(xInquiry);
		xSelector.addDecision(xAsk);
		xModule.addPiece(xSelector);
		return xSelector;
	}

	private static final XFeatureSelector makeFormTypeSelector(XModule xModule, Set<OWLClass> answerClassSet, String id) {
		XFeatureSelector xSelector = new XFeatureSelector(xModule.getDocument());
		xSelector.setId(id);
		XInquiryCall xInquiryCall = makeInquiryCall(xModule, id);
		XDecision xAsk = makeAsk(xModule, xInquiryCall, answerClassSet);
		XInquiry xInquiry = makeInquiry(xModule, answerClassSet, id);
		for (OWLClass answerClass : answerClassSet) {
			String fragment = answerClass.getIRI().getFragment().toLowerCase();
			xInquiry.addAnswer(fragment, fragment);
		}
		xModule.addPiece(xInquiry);
		xSelector.addDecision(xAsk);
		xModule.addPiece(xSelector);
		return xSelector;
	}

	private static final XInquiry makeInquiry(XModule xModule, Set<OWLClass> answerClassSet, String id) {
		XInquiry xInquiry = new XInquiry(xModule.getDocument());
		xInquiry.beAskOperator();
		xInquiry.setId(id.substring(0, id.length() - 9) + "-Q");
		xInquiry.setDefaultAnswer(answerClassSet.iterator().next().getIRI().getFragment().toLowerCase());
		xInquiry.addParamSlot(AssociationValuesSlot.WORD);
		for (OWLClass answerClass : answerClassSet) {
			String fragment = answerClass.getIRI().getFragment().toLowerCase();
			xInquiry.addAnswer(fragment);
		}
		return xInquiry;
	}

	private static final XInquiryCall makeInquiryCall(XModule xModule, String id) {
		XInquiryCall xInquiryCall = new XInquiryCall(xModule.getDocument());
		xInquiryCall.setInquiryIndex(id.substring(0, id.length() - 9) + "-Q");
		xInquiryCall.addParamIndex("Onus");
		return xInquiryCall;
	}

	private static final XDecision makeAsk(XModule xModule, XInquiryCall xInquiryCall, Set<OWLClass> answerClassSet) {
		XDecision xAsk = new XDecision(xModule.getDocument());
		xAsk.beAsk();
		xAsk.setInquiryCall(xInquiryCall);
		for (OWLClass answerClass : answerClassSet) {
			String fragment = answerClass.getIRI().getFragment().toLowerCase();
			XDecision xDecision = new XDecision(xModule.getDocument());
			xDecision.beSelect();
			xDecision.setParam1(fragment);
			List<Decision> decisionList = new LinkedList<Decision>();
			decisionList.add(xDecision);
			xAsk.addDecisionList(fragment, decisionList);
		}
		return xAsk;
	}

}

class OntologyBundle {
	OntologyWrapper wordWrapper;
	OntologyWrapper wordSsWrapper;
	OntologyWrapper wordingWrapper;
	OntologyWrapper wordingSsWrapper;
}
