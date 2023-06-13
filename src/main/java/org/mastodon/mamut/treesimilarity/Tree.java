package org.mastodon.mamut.treesimilarity;

import java.util.List;
import java.util.Set;

public interface Tree< T >
{
	/**
	 * Get the children of the tree.
	 *
	 * @return The list of child Tree objects.
	 */
	Set< Tree< T > > getChildren();

	/**
	 * Get the attributes of the tree.
	 *
	 * @return The map of attributes.
	 */
	T getAttribute();

	/**
	 * Returns a list of all descendant subtrees of this {@link Tree}.
	 *
	 * @return The list of subtrees.
	 */
	List< Tree< T > > listOfSubtrees();

	default boolean isLeaf()
	{
		return getChildren().isEmpty();
	}
}
