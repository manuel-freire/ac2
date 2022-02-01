/*
 * AC - A source-code copy detector
 *
 *     For more information please visit: http://github.com/manuel-freire/ac2
 *
 * ****************************************************************************
 *
 * This file is part of AC, version 2.x
 *
 * AC is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * AC is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with AC.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.ucm.fdi.clover.gui;

import es.ucm.fdi.clover.model.BaseGraph;
import es.ucm.fdi.clover.model.ClusterHierarchy;
import es.ucm.fdi.clover.model.ClusterViewGraph;
import es.ucm.fdi.clover.model.ClusteredGraph;
import es.ucm.fdi.clover.model.ClusteringEngine;
import es.ucm.fdi.clover.model.Filter;
import es.ucm.fdi.clover.model.FilteredGraph;
import es.ucm.fdi.clover.view.ClusterView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * This class represents saved data for a given graph. It may eventually make it
 * into the original Clover, but for now it's separate.
 * It should be subclassed for particular applications.
 *
 * The goal is to save/load a series of views (including filter settings, layouts
 * and so on) with minimal hassle.
 * Usage:
 *
 * File format is:
 * <pre>
 *     <?xml prefix?>
 *        <clover version="String" requires="String" date="Date">
 *            <shared>
 *                <filter id="ID" className="Class">
 *                    (processed by class)
 *                </filter> *
 *                <hierarchy id="ID" filterID="FID" rootVertex="vid"> +
 *                    <cluster name="String"? >
 *                        (cluster | vertex) *
 *                        <vertex id="VID">
 *                    </cluster>
 *                    <clusteringEngine id="ID" className="Class">
 *                        (processed by class)
 *                    </clusteringEngine> *
 *                </hierarchy>
 *            </shared>
 *            <view name="String" hierarchyId="HID">
 *                (processed by ClusterView; created via protected builder)
 *                <layoutCache>
 *                    (processed by LayoutCache)
 *                </layoutCache>
 *                <animatorProps>
 *                    (processed by Animator)
 *                </animatorProps>
 *            </view> +
 *        </clover>
 * </pre>
 *
 * @author mfreire
 */
public class CloverSave {

	private static Log log = LogFactory.getLog(CloverSave.class);

	protected static String saveVersion = "1.0c";
	protected static String compatibleWith = "1.0b";

	private static int idGen = 0;

	/**
	 * Saves the CloverSave to an XML file
	 */
	public static void save(Collection<ClusterView> views, File f)
			throws IOException {
		Element root = new Element("clover");
		root.setAttribute("version", saveVersion);
		root.setAttribute("requiresVersion", compatibleWith);
		root.setAttribute("date", new Date().toString());

		Element shared = new Element("shared");
		root.addContent(shared);

		HashMap<Filter, String> filterToId = new HashMap<Filter, String>();
		HashMap<ClusteringEngine, String> engineToId = new HashMap<ClusteringEngine, String>();
		HashMap<ClusterHierarchy, String> hierarchyToId = new HashMap<ClusterHierarchy, String>();

		for (ClusterView v : views) {
			Element view = new Element("view");
			v.save(view);

			Element layoutCache = new Element("layoutCache");
			v.getAnimator().getLayoutCache().save(layoutCache, v.getBase());
			view.addContent(layoutCache);

			Element animatorProps = new Element("animatorProps");
			v.getAnimator().save(animatorProps);
			view.addContent(animatorProps);

			ClusteredGraph cg = (ClusteredGraph) v.getBase();
			ClusterHierarchy h = cg.getHierarchy();
			BaseGraph bg = cg.getBase();

			// create the hierarchy element (if necessary)
			if (!hierarchyToId.containsKey(h)) {
				String hierarchyId = "" + generateId();
				hierarchyToId.put(h, hierarchyId);
				Element hierarchy = new Element("hierarchy");
				hierarchy.setAttribute("id", hierarchyId);
				h.save(hierarchy);

				// save the filtering used in the hierarchy (if necessary)
				if (bg instanceof FilteredGraph) {
					Filter filter = ((FilteredGraph) bg).getFilter();
					if (!filterToId.containsKey(filter)) {
						String filterId = "" + generateId();
						filterToId.put(filter, filterId);
						Element fe = new Element("filter");
						fe.setAttribute("id", filterId);
						filter.save(fe);
						shared.addContent(fe);
					}

					hierarchy.setAttribute("filterId", filterToId.get(filter));
				}

				// save the hierarchy itself: clustering and update-engine
				ClusteringEngine e = h.getEngine();
				Element engine = new Element("engine");
				engine.setAttribute("engineClass", e.getClass().getName());
				e.save(engine);
				hierarchy.addContent(engine);

				shared.addContent(hierarchy);
			}
			view.setAttribute("hierarchyId", hierarchyToId.get(h));

			root.addContent(view);
		}

		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		outputter.output(new Document(root), new FileOutputStream(f));
	}

	/**
	 * Restores the CloverSave to a different XML file; totally changes the
	 * 'views' (it is basically recreated) output array;
	 */
	public static ArrayList<ClusterView> restore(BaseGraph base,
			ClusterViewFactory vf, File f) throws IOException {

		if (vf == null) {
			vf = new DefaultClusterViewFactory();
		}

		ArrayList<ClusterView> views = new ArrayList<ClusterView>();
		HashMap<String, Filter> filters = new HashMap<String, Filter>();
		HashMap<String, ClusterHierarchy> hierarchies = new HashMap<String, ClusterHierarchy>();

		SAXBuilder builder = new SAXBuilder();
		ClassLoader loader = base.getClass().getClassLoader();

		try {
			Document doc = builder.build(f.getAbsolutePath());
			Element root = doc.getRootElement();

			Element sharedElems = (Element) root.getChildren().get(0);

			for (Element e : (List<Element>) sharedElems.getChildren()) {
				if (e.getName().equals("filter")) {
					String className = e.getAttributeValue("filterClass");
					Filter filter = (Filter) loader.loadClass(className)
							.newInstance();
					filter.restore(e);
					filters.put(e.getAttributeValue("id"), filter);
				} else if (e.getName().equals("hierarchy")) {
					Element cluster = (Element) e.getChildren().get(0);

					Element clusterer = (Element) e.getChildren().get(1);
					String className = clusterer
							.getAttributeValue("engineClass");
					ClusteringEngine engine = (ClusteringEngine) loader
							.loadClass(className).newInstance();
					engine.restore(clusterer);

					BaseGraph hb = base;
					if (e.getAttribute("filterId") != null) {
						Filter filter = filters.get(e
								.getAttributeValue("filterId"));
						hb = new FilteredGraph(base, filter);
					}

					ClusterHierarchy h = new ClusterHierarchy(e, hb, engine);
					hierarchies.put(e.getAttributeValue("id"), h);
				}
			}

			for (Element e : (List<Element>) root.getChildren()) {
				if (!e.getName().equals("view"))
					continue;

				// build view
				String hid = e.getAttributeValue("hierarchyId");
				ClusterView view = vf
						.createClusterView(hierarchies.get(hid), e);
				view.restore(e);

				Element layoutCache = (Element) e.getChildren().get(0);
				view.getAnimator().getLayoutCache().restore(layoutCache,
						view.getBase());

				Element animatorProps = (Element) e.getChildren().get(1);
				view.getAnimator().restore(animatorProps);

				views.add(view);
			}
		}

		// indicates a well-formedness error
		catch (JDOMException jde) {
			log.warn("Document is not well-formed XML");
			jde.printStackTrace();
		} catch (IOException ioe) {
			System.out.println(ioe);
			ioe.printStackTrace();
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}

		return views;
	}

	private static int generateId() {
		return ++idGen;
	}

	/**
	 * Nice for subclassing / hassleless programming
	 */
	public static class DefaultClusterViewFactory implements ClusterViewFactory {
		public ClusterView createClusterView(ClusterHierarchy h, Element e) {
			ClusterView cv = new ClusterView(new ClusterViewGraph(
					new ClusteredGraph(h)));
			return cv;
		}
	}

	/**
	 * Used to tell the restore method how to build a view for a given cluster hierarchy
	 * (after the base graph has been filtered and clustered and the cluster focus adjusted
	 * and all that)
	 */
	interface ClusterViewFactory {
		public ClusterView createClusterView(ClusterHierarchy h, Element e);
	}
}
