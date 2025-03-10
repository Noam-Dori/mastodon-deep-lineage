package org.mastodon.mamut.clustering.util;

import com.apporiented.algorithm.clustering.Cluster;
import com.apporiented.algorithm.clustering.ClusteringAlgorithm;
import com.apporiented.algorithm.clustering.DefaultClusteringAlgorithm;
import com.apporiented.algorithm.clustering.LinkageStrategy;
import net.imglib2.parallel.Parallelization;
import org.apache.commons.lang3.tuple.Pair;
import org.mastodon.mamut.clustering.config.SimilarityMeasure;
import org.mastodon.mamut.treesimilarity.ZhangUnorderedTreeEditDistance;
import org.mastodon.mamut.treesimilarity.tree.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ClusterUtils
{

	private ClusterUtils()
	{
		// prevent from instantiation
	}

	private static final Logger logger = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

	private static final ClusteringAlgorithm algorithm = new DefaultClusteringAlgorithm();

	/**
	 * Computes a symmetric quadratic distance matrix for the given trees using the given similarity measure. The diagonals are set to zero.
	 * @param trees a list of trees
	 * @param similarityMeasure the similarity measure to be used
	 * @return a symmetric quadratic distance matrix
	 */
	public static double[][] getDistanceMatrix( List< Tree< Double > > trees, SimilarityMeasure similarityMeasure )
	{
		int size = trees.size();
		double[][] distances = new double[ size ][ size ];
		List< Pair< Integer, Integer > > pairs = new ArrayList<>();

		// NB: only the upper triangle needs to be computed since the matrix is symmetric
		for ( int i = 0; i < size; i++ )
			for ( int j = i; j < size; j++ )
			{
				if ( i == j )
					distances[ i ][ j ] = 0; // Set diagonal elements to zero
				else
					pairs.add( Pair.of( i, j ) );
			}

		Parallelization.getTaskExecutor().forEach( pairs, pair -> {
			int i = pair.getLeft();
			int j = pair.getRight();
			double distance = similarityMeasure.compute( trees.get( i ), trees.get( j ),
					ZhangUnorderedTreeEditDistance.DEFAULT_COST_FUNCTION );
			distances[ i ][ j ] = distance;
			distances[ j ][ i ] = distance; // symmetric
		} );

		return distances;
	}

	/**
	 * Gets a {@link Classification} that contains a mapping from cluster ids to objects.<p>
	 * The cluster ids are incremented by 1 starting from 0.
	 * The amount of clusters depends on the given threshold.
	 * <p>
	 * Constraints:
	 * <ul>
	 *     <li>The distance matrix needs to be quadratic</li>
	 *     <li>The distance matrix to be symmetric with zero diagonal</li>
	 *     <li>The number of object needs to equal the length of the distance matrix</li>
	 * </ul>
	 *
	 * @param objects the objects to be clustered
	 * @param distances the symmetric distance matrix with zero diagonal
	 * @param linkageStrategy the linkage strategy (e.g. {@link com.apporiented.algorithm.clustering.AverageLinkageStrategy}, {@link com.apporiented.algorithm.clustering.CompleteLinkageStrategy}, {@link com.apporiented.algorithm.clustering.SingleLinkageStrategy})
	 * @param threshold the threshold for the distance for building clusters
	 * @return a mapping from cluster id objects
	 */
	public static < T > Classification< T > getClassificationByThreshold(
			final T[] objects, final double[][] distances, final LinkageStrategy linkageStrategy, final double threshold
	)
	{
		return getClassificationByThreshold( objects, distances, linkageStrategy, threshold, null, null, null );
	}

	private static < T > Classification< T > getClassificationByThreshold(
			final T[] objects, final double[][] distances, final LinkageStrategy linkageStrategy, final double threshold,
			@Nullable Map< String, T > objectMapping, @Nullable Cluster algorithmResult, @Nullable List< Cluster > sortedClusters
	)
	{
		if ( threshold < 0 )
			throw new IllegalArgumentException( "threshold must be greater than or equal to zero" );

		if ( objectMapping == null )
			objectMapping = objectMapping( objects );
		if ( algorithmResult == null )
			algorithmResult = performClustering( distances, linkageStrategy, objectMapping );
		if ( sortedClusters == null )
			sortedClusters = sortClusters( algorithmResult );

		Set< Cluster > resultClusters = new HashSet<>();
		for ( Cluster cluster : sortedClusters )
		{
			if ( cluster.getDistanceValue() < threshold )
				break;
			resultClusters.add( cluster );
		}

		Set< Set< T > > classifiedObjects = convertClustersToClasses( resultClusters, objectMapping );
		log( classifiedObjects );
		return new Classification<>( classifiedObjects, algorithmResult, objectMapping, threshold );
	}

	/**
	 * Gets a {@link Classification} that contains a mapping from cluster ids to objects.<p>
	 * The cluster ids are incremented by 1 starting from 0.
	 * The amount of clusters depends on the given class count.
	 * <p>
	 * Constraints:
	 * <ul>
	 *     <li>The distance matrix needs to be quadratic</li>
	 *     <li>The distance matrix to be symmetric with zero diagonal</li>
	 *     <li>The number of objects needs to equal the length of the distance matrix</li>
	 *     <li>The class count needs to be greater than zero</li>
	 *     <li>The class count needs to be less than or equal to the number of names</li>
	 * </ul>
	 *
	 * @param objects the objects to be clustered
	 * @param distances the symmetric distance matrix with zero diagonal
	 * @param linkageStrategy the linkage strategy (e.g. {@link com.apporiented.algorithm.clustering.AverageLinkageStrategy}, {@link com.apporiented.algorithm.clustering.CompleteLinkageStrategy}, {@link com.apporiented.algorithm.clustering.SingleLinkageStrategy})
	 * @param classCount the number of classes to be built
	 * @return a mapping from cluster id objects
	 */
	public static < T > Classification< T > getClassificationByClassCount(
			final T[] objects, final double[][] distances,
			final LinkageStrategy linkageStrategy, final int classCount
	)
	{
		if ( classCount < 1 )
			throw new IllegalArgumentException( "number of classes (" + classCount + ") must be greater than zero." );
		else if ( classCount > objects.length )
			throw new IllegalArgumentException(
					"number of classes (" + classCount + ") must be less than or equal to the number of objects to be classified ("
							+ objects.length + ")." );
		else if ( classCount == 1 )
			return new Classification<>( Collections.singleton( new HashSet<>( Arrays.asList( objects ) ) ), null, null, 0d );

		Set< Set< T > > classes = new HashSet<>();
		if ( classCount == objects.length )
		{
			for ( T name : objects )
				classes.add( Collections.singleton( name ) );
			return new Classification<>( classes, null, null, 0d );
		}

		// NB: the cluster algorithm needs unique names instead of objects
		Map< String, T > objectMapping = objectMapping( objects );
		Cluster algorithmResult = performClustering( distances, linkageStrategy, objectMapping );
		List< Cluster > sortedClusters = sortClusters( algorithmResult );
		double threshold = getThreshold( sortedClusters, classCount );

		return getClassificationByThreshold(
				objects, distances, linkageStrategy, threshold, objectMapping, algorithmResult, sortedClusters );
	}

	private static double getThreshold( final List< Cluster > sortedClusters, int classCount )
	{
		double threshold = sortedClusters.get( classCount - 2 ).getDistanceValue();
		if ( sortedClusters.size() < classCount )
			return threshold;
		else
			return ( threshold + sortedClusters.get( classCount - 1 ).getDistanceValue() ) / 2d;
	}

	private static < T > Cluster performClustering( double[][] distances, LinkageStrategy linkageStrategy,
			Map< String, T > uniqueObjectNames )
	{
		String[] uniqueNames = uniqueObjectNames.keySet().toArray( new String[ 0 ] );
		return algorithm.performClustering( distances, uniqueNames, linkageStrategy );
	}

	private static List< Cluster > sortClusters( Cluster algorithmResult )
	{
		List< Cluster > clusters = allClusters( algorithmResult );
		clusters.sort( Comparator.comparingDouble( Cluster::getDistanceValue ) );
		Collections.reverse( clusters );
		return clusters;
	}

	private static < T > Map< String, T > objectMapping( T[] objects )
	{
		Map< String, T > objectNames = new LinkedHashMap<>();
		for ( int i = 0; i < objects.length; i++ )
			objectNames.put( String.valueOf( i ), objects[ i ] );
		return objectNames;
	}

	private static < T > Set< Set< T > > convertClustersToClasses( Set< Cluster > output, Map< String, T > objectNames )
	{
		int clusterId = 0;
		Map< Integer, List< String > > classes = new HashMap<>();
		for ( Cluster cluster : output )
		{
			for ( Cluster child : cluster.getChildren() )
			{
				if ( !output.contains( child ) )
					classes.put( clusterId++, leaveNames( child ) );
			}
		}
		Set< Set< T > > classifiedObjects = new HashSet<>();
		for ( Map.Entry< Integer, List< String > > entry : classes.entrySet() )
		{
			Set< T > objects = new HashSet<>();
			for ( String name : entry.getValue() )
				objects.add( objectNames.get( name ) );
			classifiedObjects.add( objects );
		}
		return classifiedObjects;
	}

	private static < T > void log( Set< Set< T > > objectsToClusterIds )
	{
		int i = 0;
		for ( Set< T > entry : objectsToClusterIds )
		{
			if ( logger.isInfoEnabled() )
				logger.info( "clusterId: {}, object: {}", i++,
						entry.stream().map( Object::toString ).collect( Collectors.joining( "," ) )
				);
		}
	}

	private static List< Cluster > allClusters( final Cluster cluster )
	{
		List< Cluster > list = new ArrayList<>();
		list.add( cluster );
		for ( Cluster child : cluster.getChildren() )
			list.addAll( allClusters( child ) );
		return list;
	}

	private static List< String > leaveNames( final Cluster cluster )
	{
		List< String > list = new ArrayList<>();
		if ( cluster.isLeaf() )
			list.add( cluster.getName() );
		for ( Cluster child : cluster.getChildren() )
			list.addAll( leaveNames( child ) );
		Collections.sort( list );
		return list;
	}
}
