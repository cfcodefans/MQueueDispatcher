package com.thenetcircle.services.commons.web.mvc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.jsoup.nodes.Element;

public class ViewProcModel implements Serializable {
	
	public static class ScriptCtxModel implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private final Element scriptElement;
		
		private final Integer[] path;

		public ScriptCtxModel(Element root, Element scriptElement) {
			super();
			this.scriptElement = scriptElement;
			this.path = elementPath(root, scriptElement);
		}
		
		public Element getScriptElementByPath(final Element cloneOfRoot) {
			return getElementByPath(cloneOfRoot, path);
		}
	}
	
	public static class ViewFacade {
		public final Element _doc;
		public final List<Element> scriptElementList;
		public ViewFacade(Element _doc, List<Element> scriptElementList) {
			super();
			this._doc = _doc;
			this.scriptElementList = scriptElementList;
		}
	}
	
	public ViewFacade getViewFacade() {
		final Element _doc = doc.clone();
		List<Element> _scriptElementList = scriptCtxModelList.stream().map(cm->cm.getScriptElementByPath(_doc)).collect(Collectors.toList());
//		final List<Element> _scriptElementList = new LinkedList<Element>();
//		for (ScriptCtxModel cm : scriptCtxModelList) {
//			_scriptElementList.add(cm.getScriptElementByPath(_doc));
//		}
		return new ViewFacade(_doc, _scriptElementList);
	}
	
	private static final long serialVersionUID = 1L;

	private final Element doc;
	
	private final List<ScriptCtxModel> scriptCtxModelList = new ArrayList<ScriptCtxModel>();

	public ViewProcModel(Element domRoot) {
		super();
		this.doc = domRoot;
	} 
	
	public List<ScriptCtxModel> getScriptCtxModelList() {
		return scriptCtxModelList;
	}

	public static Integer[] elementPath(Element root, Element _element) {
		if (root == null || _element == null) {
			return new Integer[0];
		}
		
		LinkedList<Integer> idxList = new LinkedList<Integer>();
		
		for (Element currentElement = _element; currentElement != root; currentElement = currentElement.parent()) {
			if (currentElement == null) {
				// the root is not ancestor of _element
				return new Integer[0];
			}
			
			idxList.addFirst(currentElement.elementSiblingIndex());
		}
		
		return idxList.toArray(new Integer[0]);
	}
	
	public static Element getElementByPath(final Element start, Integer[] path) {
		if (start == null || ArrayUtils.isEmpty(path)) {
			return null;
		}
		
		Element _element = start;
		for (Integer idx : path) {
			_element = _element.child(idx);
			if (_element == null) {
				// wrong path
				return null;
			}
		}
		
		return _element;
	}
}
