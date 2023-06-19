package org.mastodon.mamut.treesimilarity.tree;

import org.mastodon.mamut.model.branch.BranchLink;
import org.mastodon.mamut.model.branch.BranchSpot;

import java.util.HashSet;
import java.util.Set;

public class BranchSpotTree implements Tree< Double >
{
	private final BranchSpot branchSpot;

	private final double endTimepoint;

	private final HashSet< Tree< Double > > children;

	public BranchSpotTree( final BranchSpot branchSpot, final double endTimepoint )
	{
		this.branchSpot = branchSpot;
		this.endTimepoint = endTimepoint;
		this.children = new HashSet<>();
		for ( BranchLink branchLink : branchSpot.outgoingEdges() )
		{
			BranchSpot child = branchLink.getTarget();
			if ( child.getFirstTimePoint() <= this.endTimepoint )
				this.children.add( new BranchSpotTree( child, this.endTimepoint ) );
		}
	}

	@Override
	public Set< Tree< Double > > getChildren()
	{
		return children;
	}

	@Override
	public Double getAttribute()
	{
		double lifespan;
		if ( branchSpot.getTimepoint() > this.endTimepoint )
			lifespan = this.endTimepoint - branchSpot.getFirstTimePoint();
		else
			lifespan = branchSpot.getTimepoint() - branchSpot.getFirstTimePoint();
		return lifespan;
	}

	public BranchSpot getBranchSpot()
	{
		return branchSpot;
	}
}
