package org.mastodon.mamut.treesimilarity.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.mastodon.mamut.treesimilarity.ZhangUnorderedTreeEditDistance;
import org.mastodon.mamut.treesimilarity.tree.SimpleTree;
import org.mastodon.mamut.treesimilarity.tree.SimpleTreeExamples;
import org.mastodon.mamut.treesimilarity.tree.Tree;
import org.mastodon.mamut.treesimilarity.tree.TreeUtils;

public class NodeMappingTest
{

	private static final BiFunction< Double, Double, Double > DEFAULT_COSTS = ( o1, o2 ) -> {
		if ( o2 == null )
			return o1;
		else
			return Math.abs( o1 - o2 );
	};

	@Test
	public void testTree1vs2()
	{
		testNodeMapping( SimpleTreeExamples.tree1(), SimpleTreeExamples.tree2(),
				20, "10->10, 20->30, 30->20" );
	}

	@Test
	public void testTree3vs4()
	{
		testNodeMapping( SimpleTreeExamples.tree3(), SimpleTreeExamples.tree4(),
				4, "1->1, 1->1, 100->100" );
	}

	@Test
	public void testTree5vs6()
	{
		testNodeMapping( SimpleTreeExamples.tree5(), SimpleTreeExamples.tree6(),
				49, "13->12, 203->227, 203->227" );
	}

	@Test
	public void testTree5vs7()
	{
		testNodeMapping( SimpleTreeExamples.tree5(), SimpleTreeExamples.tree7(),
				69, "13->12, 203->227, 203->227" );
	}

	@Test
	public void testTree8vs9()
	{
		testNodeMapping( SimpleTreeExamples.tree8(), SimpleTreeExamples.tree9(),
				1, "1->1, 2->2, 3->3, 4->4, 5->4, 8->8, 8->8" );
	}

	@Test
	public void testTree12vs13()
	{
		testNodeMapping( SimpleTreeExamples.tree12(), SimpleTreeExamples.tree13(),
				2, "100->100, 1000->1000, 200->200" );
	}

	@Test
	public void testNonBinaryTree()
	{
		testNodeMapping( SimpleTreeExamples.tree14(), SimpleTreeExamples.nonBinaryTree(),
				1_000_003, "10000->10001, 2->3, 4->5" );
	}

	@Test
	public void testTree15vs16()
	{
		testNodeMapping( SimpleTreeExamples.tree15(), SimpleTreeExamples.tree16(),
				203, "1000->1000, 200->200, 300->300" );
	}

	@Test
	public void testTree18vs19()
	{
		testNodeMapping( SimpleTreeExamples.tree18(), SimpleTreeExamples.tree19(),
				21, "100->100, 200->200, 300->300" );
	}

	@Test
	public void testEmptyTrees()
	{
		testNodeMapping( SimpleTreeExamples.tree1(), null, 60, "" );
		testNodeMapping( null, null, 0, "" );
	}

	@Test
	public void testLeaves()
	{
		testNodeMapping( new SimpleTree<>( 10d ), null, 10, "" );
		testNodeMapping( new SimpleTree<>( 10d ), new SimpleTree<>( 20d ), 10, "10->20" );
	}

	private void testNodeMapping( Tree< Double > tree1, Tree< Double > tree2, double expectedCosts, String expectedMapping )
	{
		testNodeMappingForward( tree1, tree2, expectedCosts, expectedMapping );
		testNodeMappingForward( tree2, tree1, expectedCosts, revertExpectedMapping( expectedMapping ) );
	}

	private void testNodeMappingForward( Tree< Double > tree1, Tree< Double > tree2, double expectedCosts, String expectedMapping )
	{
		Map< Tree< Double >, Tree< Double > > mapping = ZhangUnorderedTreeEditDistance.nodeMapping( tree1, tree2, DEFAULT_COSTS );
		assertEquals( expectedMapping, asString( mapping ) );
		assertEquals( expectedCosts, computeCosts( tree1, tree2, mapping ), 0.0 );
	}

	private String revertExpectedMapping( String expectedMapping )
	{
		if ( expectedMapping.isEmpty() )
			return expectedMapping;

		return Stream.of( expectedMapping.split( ", " ) ).map( s -> {
			String[] split = s.split( "->" );
			return split[ 1 ] + "->" + split[ 0 ];
		} ).sorted().collect( Collectors.joining( ", " ) );
	}

	private double computeCosts( Tree< Double > tree1, Tree< Double > tree2, Map< Tree< Double >, Tree< Double > > mapping )
	{
		Set< Tree< Double > > keys = mapping.keySet();
		Set< Tree< Double > > values = new HashSet<>( mapping.values() );
		double costs = 0;
		for ( Tree< Double > subtree : TreeUtils.listOfSubtrees( tree1 ) )
			if ( !keys.contains( subtree ) )
				costs += DEFAULT_COSTS.apply( subtree.getAttribute(), null );
		for ( Tree< Double > subtree : TreeUtils.listOfSubtrees( tree2 ) )
			if ( !values.contains( subtree ) )
				costs += DEFAULT_COSTS.apply( subtree.getAttribute(), null );
		for ( Map.Entry< Tree< Double >, Tree< Double > > entry : mapping.entrySet() )
			costs += DEFAULT_COSTS.apply( entry.getKey().getAttribute(), entry.getValue().getAttribute() );
		return costs;
	}

	private String asString( Map< Tree< Double >, Tree< Double > > mapping )
	{
		ArrayList< String > strings = new ArrayList<>();
		mapping.forEach( ( key, value ) -> strings.add( Math.round( key.getAttribute() ) + "->" + Math.round( value.getAttribute() ) ) );
		Collections.sort( strings );
		return String.join( ", ", strings );
	}
}
