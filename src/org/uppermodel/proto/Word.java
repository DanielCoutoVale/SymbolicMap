package org.uppermodel.proto;

import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLIndividual;

/**
 * A word in a descriptive format.
 * 
 * @author Daniel Couto Vale <danielvale@uni-bremen.de>
 */
public class Word {

	public final OWLIndividual id;
	public final Set<OWLClass> classSet;
	public final Map<String, String> stemMap;

	public Word(OWLIndividual id, Set<OWLClass> classSet, Map<String, String> patternMap) {
		this.id = id;
		this.classSet = classSet;
		this.stemMap = patternMap;
	}

}