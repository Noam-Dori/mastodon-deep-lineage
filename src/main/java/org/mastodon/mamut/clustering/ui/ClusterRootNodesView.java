package org.mastodon.mamut.clustering.ui;

import org.mastodon.mamut.clustering.ClusterRootNodesController;
import org.mastodon.mamut.clustering.config.ClusteringMethod;
import org.mastodon.mamut.clustering.config.CropCriteria;
import org.mastodon.mamut.clustering.config.SimilarityMeasure;
import org.scijava.ItemVisibility;
import org.scijava.command.InteractiveCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

@Plugin(type = InteractiveCommand.class, visible = false, label = "Classification of Lineage Trees")
public class ClusterRootNodesView extends InteractiveCommand
{

	private static final int WIDTH = 15;

	private static final int WIDTH_INPUT = 7;


	@SuppressWarnings("unused")
	@Parameter
	private ClusterRootNodesController controller;

	@SuppressWarnings("all")
	@Parameter(visibility = ItemVisibility.MESSAGE, required = false, persist = false)
	private String documentation = "<html>\n"
			+ "<body width=" + WIDTH + "cm align=left>\n"
			+ "<h1>Classification of Lineage Trees</h1>\n"
			+ "<p>This plugin is capable in grouping similar lineage trees together. This is done by creating a tag set an assigning subtrees that are similar to each other with the same tag.</p>\n"
			+ "<p>The similarity between two subtrees is computed based on the Zhang edit distance for unordered trees <a href=\"https://doi.org/10.1007/BF01975866\">(Zhang, K. Algorithmica 15, 205–222, 1996)</a>. The similarity measure uses the attribute the cell lifetime, which is computed as a difference of timepoints between to subsequent divisions. It is possible to apply the <i>absolute difference</i>, <i>average difference</i> or the <i>normalized difference</i> of cell lifetimes.</p>\n"
			+ "<p>The similarity is computed between all possible combinations of subtrees leading to a two-dimensional similarity matrix. This matrix is then used to perform a <a href=\"https://en.wikipedia.org/wiki/Hierarchical_clustering\">agglomerative hierarchical clustering</a> into a specifiable number of classes. For the clustering three different <a href=\"https://en.wikipedia.org/wiki/Hierarchical_clustering#Cluster_Linkage\">linkage methods</a> can be chosen.</p>\n"
			+ "</body>\n"
			+ "</html>\n";

	@SuppressWarnings("unused")
	@Parameter(label = "Crop criterion", choices = { "Timepoint", "Number of cells" }, callback = "update")
	private String cropCriterion;

	@SuppressWarnings("unused")
	@Parameter(label = "Crop start", min = "0", callback = "update")
	private int start;

	@SuppressWarnings("unused")
	@Parameter(label = "Crop end", min = "0", callback = "update")
	private int end;

	@SuppressWarnings("unused")
	@Parameter(label = "Number of classes", min = "1", callback = "update")
	private int numberOfClasses;

	@SuppressWarnings("unused")
	@Parameter(label = "Minimum number of cell divisions", min = "0", callback = "update")
	private int numberOfCellDivisions;

	@SuppressWarnings("unused")
	@Parameter(
			label = "Similarity measure", choices = { "Normalized Zhang Tree Distance", "Per cell Zhang Tree Distance",
			"Zhang Tree Distance" }, callback = "update"
	)
	private String similarityMeasure;

	@SuppressWarnings("unused")
	@Parameter(
			label = "Linkage strategy for hierarchical clustering", choices = { "Average linkage", "Single Linkage",
			"Complete Linkage" }, callback = "update"
	)
	private String clusteringMethod;

	@SuppressWarnings("unused")
	@Parameter(label = "Feature", choices = "Cell lifetime", callback = "update")
	private String cellLifeTime;
	// NB: dynamic choices: https://github.com/imagej/tutorials/blob/c78764438d774295d00fc8a4273e4c4f25c8ad46/maven-projects/dynamic-commands/src/main/java/DynamicCallbacks.java

	@SuppressWarnings("unused")
	@Parameter(label = "Show dendrogram of clustering", callback = "update")
	private boolean showDendrogram;

	@SuppressWarnings("unused")
	@Parameter(visibility = ItemVisibility.MESSAGE, required = false, persist = false, label = " ")
	private String paramFeedback;

	@SuppressWarnings("unused")
	@Parameter(visibility = ItemVisibility.MESSAGE, required = false, persist = false, label = " ")
	private String computeFeedback;

	@SuppressWarnings("unused")
	@Parameter(label = "Create tag set", callback = "createTagSet", persist = false)
	private Button createTagSet;

	/**
	 * This method is executed whenever a parameter changes
	 */
	@Override
	public void run()
	{
		// NB: not implemented. Update method is called via callback on each parameter change.
	}

	@SuppressWarnings("unused")
	private void update()
	{
		controller.setInputParams( CropCriteria.getByName( cropCriterion ), start, end, numberOfCellDivisions );
		controller.setComputeParams(
				SimilarityMeasure.getByName( similarityMeasure ), ClusteringMethod.getByName( clusteringMethod ), numberOfClasses );
		controller.setShowDendrogram( showDendrogram );

		paramFeedback = "<html><body width=" + WIDTH_INPUT + "cm>";
		if ( controller.isValidParams() )
			paramFeedback += "<font color=green>Parameters are valid.";
		else
		{
			paramFeedback += "<font color=red>" + String.join( "<p>", controller.getFeedback() );
		}
		paramFeedback += "</font></body></html>";
	}

	@SuppressWarnings("unused")
	private void createTagSet()
	{
		update();
		if ( controller.isValidParams() )
		{
			String feedback;
			try
			{
				controller.createTagSet();
				feedback = "Tag set created.";
			}
			catch ( IllegalArgumentException e )
			{
				feedback = e.getMessage();
			}
			computeFeedback = "<html><body width=" + WIDTH_INPUT + "cm><font color=\"green\">" + feedback + "</font></body></html>";
		}
	}
}
