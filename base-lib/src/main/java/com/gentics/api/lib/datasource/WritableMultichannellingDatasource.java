package com.gentics.api.lib.datasource;

/**
 * Interface for writeable multichannelling datasources
 */
public interface WritableMultichannellingDatasource extends MultichannellingDatasource, WriteableDatasource {

	/**
	 * Name of the datamap parameter for the channelset_id when creating an object for multichannelling
	 */
	public final static String MCCR_CHANNELSET_ID = "com.gentics.contentnode.mccr.channelset_id";

	/**
	 * Name of the datamap parameter for the channel_id when creating an object for multichannelling
	 */
	public final static String MCCR_CHANNEL_ID = "com.gentics.contentnode.mccr.channel_id";

	/**
	 * Save the given channel structure to the datasource. All channels not mentioned in the structure, will be removed from the datasource.
	 * @param root root
	 * @throws DatasourceException in case of errors
	 */
	void saveChannelStructure(ChannelTree root) throws DatasourceException;
}
