package com.gentics.api.lib.datasource;

import java.util.List;

/**
 * Interface for a multichannelling aware Datasource
 */
public interface MultichannellingDatasource extends Datasource {

	/**
	 * Set the channel to be used. When the channel with given id cannot be set, an exception is thrown and the previous selection will remain.
	 * @param channelId id of the channel
	 * @return channel object (never null)
	 * @throws DatasourceException when the channel id cannot be set
	 */
	DatasourceChannel setChannel(int channelId) throws DatasourceException;

	/**
	 * Get the currently used channels (one for each separate channel structure).
	 * This list will be null if no channel is contained in the mccr
	 * @return list of channel objects
	 * @throws DatasourceException in case of errors
	 */
	List<DatasourceChannel> getChannels() throws DatasourceException;

	/**
	 * Get the list of paths to the currently selected channels. In every path. the first entry will be the master node, the last entry the currently used channel.
	 * @return paths to the currently selected channels
	 * @throws DatasourceException in case of errors
	 */
	List<List<DatasourceChannel>> getChannelPaths() throws DatasourceException;

	/**
	 * Get the whole structure of nodes and channels currently contained in the datasource
	 * @return root node (containing the whole node structure)
	 * @throws DatasourceException in case of errors
	 */
	ChannelTree getChannelStructure() throws DatasourceException;
}
