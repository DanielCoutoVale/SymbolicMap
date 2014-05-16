package org.uppermodel.tools;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

/**
 * Wraps an OWL ontology
 * 
 * @author Daniel Couto Vale <danielvale@uni-bremen.de>
 */
public class OntologyWrapper {

	/**
	 * The wrapped ontology
	 */
	OWLOntology ontology;

	/**
	 * Constructor
	 * 
	 * @param ontology the ontology to wrap
	 */
	public OntologyWrapper(OWLOntology ontology) {
		this.ontology = ontology;
	}

	/**
	 * Fetches all disjoint unions of a class.
	 * 
	 * @param oUnionClass the union-class
	 * @return
	 */
	public final Set<Set<OWLClass>> fetchDisjointUnions(OWLClass oUnionClass) {
		Set<Set<OWLClass>> oDisjointUnionSet = new HashSet<Set<OWLClass>>(); 
		for (OWLDisjointUnionAxiom axiom : ontology.getDisjointUnionAxioms(oUnionClass)) {
			Set<OWLClass> oDisjointUnion = axiom.getOWLDisjointClassesAxiom().getClassesInSignature();
			oDisjointUnionSet.add(oDisjointUnion);
		}
		return oDisjointUnionSet;
	}

	/**
	 * Fetches all disjoint unions of a class by definition.
	 * 
	 * @param oUnionClass the union-class
	 * @return
	 */
	public final Set<Set<OWLClass>> fetchDisjointUnionsByDefinition(OWLClass oUnionClass, boolean defined) {
		Set<Set<OWLClass>> oDisjointUnionSet = new HashSet<Set<OWLClass>>(); 
		for (Set<OWLClass> oDisjointUnion : fetchDisjointUnions(oUnionClass)) {
			oDisjointUnion = selectClassesByDefinition(oDisjointUnion, defined);
			if (oDisjointUnion.size() > 0) {
				oDisjointUnionSet.add(oDisjointUnion);
			}
		}
		return oDisjointUnionSet;
	}

	/**
	 * Fetches all subclasses of a particular class.
	 * 
	 * @param oSuperClass the super-class
	 * @return the set of sub-classes 
	 */
	public final Set<OWLClass> fetchSubClasses(OWLClass oSuperClass) {
		Set<OWLClass> oSubClassSet = new HashSet<OWLClass>();
		Set<OWLSubClassOfAxiom> axiomSet = ontology.getSubClassAxiomsForSuperClass(oSuperClass);
		for (OWLSubClassOfAxiom axiom : axiomSet) {
			OWLClass oSubClass = (OWLClass) axiom.getSubClass();
			oSubClassSet.add(oSubClass);
		}
		return oSubClassSet;
	}

	/**
	 * Fetches all super-classes of a particular class.
	 * 
	 * @param oSuperClass the sub-class
	 * @return the set of super-classes 
	 */
	public final Set<OWLClass> fetchSuperClasses(OWLClass oSubClass) {
		Set<OWLClass> oSuperClassSet = new HashSet<OWLClass>();
		Set<OWLSubClassOfAxiom> axiomSet = ontology.getSubClassAxiomsForSubClass(oSubClass);
		for (OWLSubClassOfAxiom axiom : axiomSet) {
			OWLClassExpression oSuperClassExp = axiom.getSuperClass();
			if (oSuperClassExp instanceof OWLClass) {
				OWLClass oSuperClass = (OWLClass) axiom.getSuperClass();
				oSuperClassSet.add(oSuperClass);
			}
		}
		return oSuperClassSet;
	}

	/**
	 * Selects all undefined classes of a class set.
	 * 
	 * @param oClassSet the set of classes
	 * @param defined whether the class is to be defined
	 * @return the set of (un-)defined classes
	 */
	public final Set<OWLClass> selectClassesByDefinition(Set<OWLClass> oClassSet, boolean defined) {
		if (defined) {
			return selectDefinedClasses(oClassSet);
		} else {
			return selectUndefinedClasses(oClassSet);
		}
	}

	/**
	 * Selects all undefined classes of a class set.
	 * 
	 * @param oClassSet the set of classes
	 * @return the set of undefined classes
	 */
	public final Set<OWLClass> selectUndefinedClasses(Set<OWLClass> oClassSet) {
		Set<OWLClass> oUndefinedClassSet = new HashSet<OWLClass>();
		for (OWLClass oClass : oClassSet) {
			if (ontology.getEquivalentClassesAxioms(oClass).size() == 0) {
				oUndefinedClassSet.add(oClass);
			}
		}
		return oUndefinedClassSet;
	}

	/**
	 * Selects all defined classes of a class set.
	 * 
	 * @param oClassSet the set of classes
	 * @return the set of defined classes
	 */
	public final Set<OWLClass> selectDefinedClasses(Set<OWLClass> oClassSet) {
		Set<OWLClass> oDefinedClassSet = new HashSet<OWLClass>();
		for (OWLClass oClass : oClassSet) {
			if (ontology.getEquivalentClassesAxioms(oClass).size() > 0) {
				oDefinedClassSet.add(oClass);
			}
		}
		return oDefinedClassSet;
	}

	/**
	 * Groups disjunct classes of a class set into sets
	 * 
	 * WARNING: we are dealing with grammatical systems, so a class can belong to no more than
	 * one disjunction. 
	 * 
	 * @param oClassSet the set of classes
	 * @return the sets of disjunct class sets
	 */
	public final Set<Set<OWLClass>> groupDisjunctClasses(Set<OWLClass> oClassSet) {
		Set<Set<OWLClass>> oDisjuctClassSetSet = new HashSet<Set<OWLClass>>();
		Stack<OWLClass> oClassStack = new Stack<OWLClass>();
		oClassStack.addAll(oClassSet);
		while (!oClassStack.empty()) {
			Set<OWLClass> oDisjuctClassSet = new HashSet<OWLClass>();
			OWLClass oClass = oClassStack.pop();
			oDisjuctClassSet.add(oClass);
			for (OWLDisjointClassesAxiom axiom : ontology.getDisjointClassesAxioms(oClass)) {
				oDisjuctClassSet.addAll(axiom.getClassesInSignature());
			}
			oDisjuctClassSetSet.add(oDisjuctClassSet);
		}
		return oDisjuctClassSetSet;
	}

}
