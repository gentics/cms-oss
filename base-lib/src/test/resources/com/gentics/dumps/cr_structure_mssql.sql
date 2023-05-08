/****** Object:  Table [dbo].[contentattribute]    Script Date: 01/16/2007 13:55:57 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[contentattribute](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[contentid] [nvarchar](32)NOT NULL default '',
	[name] [nvarchar](255) NOT NULL default '',
	[value_text] [nvarchar](255) NULL,
	[value_bin] [varbinary](max) NULL,
	[value_int] [int] NULL default NULL,
	[sortorder] [int] NULL default NULL,
	[value_blob] [varbinary](max) NULL,
	[value_clob] [ntext] NULL,
	[value_long] [bigint] NULL default NULL,
	[value_double] [float] NULL default NULL,
	[value_date] [datetime] NULL default NULL,
 CONSTRAINT [PK_contentattribute] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX  = OFF, IGNORE_DUP_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
CREATE INDEX [contentattribute_idx1]
 ON [contentattribute] ([contentid])
GO
CREATE INDEX [contentattribute_idx2]
 ON [contentattribute] ([name])
GO
CREATE INDEX [contentattribute_idx3]
 ON [contentattribute] ([contentid], [name])
GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[contentattribute_nodeversion]    Script Date: 01/16/2007 13:55:58 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[contentattribute_nodeversion](
	[id] [int] NOT NULL default '0',
	[contentid] [nvarchar](32) NOT NULL default '',
	[name] [nvarchar](255) NOT NULL default '',
	[value_text] [nvarchar](255) NULL,
	[value_bin] [varbinary](max) NULL,
	[value_int] [int] NULL default NULL,
	[sortorder] [int] NULL default NULL,
	[value_blob] [varbinary](max) NULL,
	[value_clob] [ntext] NULL,
	[value_long] [bigint] NULL default NULL,
	[value_double] [float] NULL default NULL,
	[value_date] [datetime] NULL default NULL,
	[nodeversiontimestamp] [int] NULL default NULL,
	[nodeversion_user] [nvarchar](255) NULL default NULL,
	[nodeversionlatest] [int] NULL default NULL,
	[nodeversionremoved] [int] NULL default NULL,
	[nodeversion_autoupdate] [tinyint] NOT NULL default 0
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
CREATE INDEX [contentattribute_nodever_idx1]
  ON [contentattribute_nodeversion] ([id], [nodeversiontimestamp])
GO
CREATE INDEX [contentattribute_nodever_idx2]
  ON [contentattribute_nodeversion] ([contentid], [name])
GO
CREATE INDEX [contentattribute_nodever_idx3]
  ON [contentattribute_nodeversion] ([sortorder])
GO
CREATE INDEX [contentattribute_nodever_idx5]
  ON [contentattribute_nodeversion] ([contentid])
GO
CREATE INDEX [contentattribute_nodever_idx6]
  ON [contentattribute_nodeversion] ([id])
GO
CREATE INDEX [contentattribute_nodever_idx7]
  ON [contentattribute_nodeversion] ([name])
GO
CREATE INDEX [contentattribute_nodever_idx8]
  ON [contentattribute_nodeversion] ([contentid],[name],[value_text])
GO

SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[contentattributetype]    Script Date: 01/16/2007 13:55:58 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[contentattributetype](
	[name] [nvarchar](255) NULL default NULL,
	[attributetype] [int] NULL default NULL,
	[optimized] [int] NULL default NULL,
	[multivalue] [int] NOT NULL default 0,
	[objecttype] [int] NOT NULL default 0,
	[linkedobjecttype] [int] NOT NULL default 0,
	[foreignlinkattribute] [nvarchar](255) NULL default NULL,
	[foreignlinkattributerule] [ntext] NULL,
	[exclude_versioning] [int] NOT NULL default 0,
	[quickname] [nvarchar](255) NULL default NULL,
	[filesystem] [int] NOT NULL default 0
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
/****** Object:  Table [dbo].[contentmap]    Script Date: 01/16/2007 13:55:58 ******/
-- gentics-start-table-contentmap
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[contentmap](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[contentid] [nvarchar](32) NOT NULL default '',
	[obj_id] [int] NOT NULL default 0,
	[obj_type] [int] NOT NULL default 0,
	[mother_obj_id] [int] NOT NULL default 0,
	[mother_obj_type] [int] NOT NULL default 0,
	[updatetimestamp] [int] NOT NULL default 0,
	[motherid] [nvarchar](32) NULL default NULL,
 CONSTRAINT [PK_contentmap] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX  = OFF, IGNORE_DUP_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]

GO
CREATE INDEX [contentmap_idx1]
  ON [contentmap] ([obj_id])
GO
CREATE INDEX [contentmap_idx2]
  ON [contentmap] ([obj_type])
GO
CREATE INDEX [contentmap_idx3]
  ON [contentmap] ([motherid], [contentid])
GO
CREATE UNIQUE INDEX [contentmap_idx4]
  ON [contentmap] ([contentid])
GO
-- gentics-end-table-contentmap
/****** Object:  Table [dbo].[contentmap_nodeversion]    Script Date: 01/16/2007 13:55:58 ******/
-- gentics-start-table-contentmap_nodeversion
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[contentmap_nodeversion](
	[id] [int] NOT NULL default 0,
	[contentid] [nvarchar](32) NOT NULL default '',
	[obj_id] [int] NOT NULL default 0,
	[obj_type] [int] NOT NULL default 0,
	[mother_obj_id] [int] NOT NULL default 0,
	[mother_obj_type] [int] NOT NULL default 0,
	[updatetimestamp] [int] NOT NULL default 0,
	[motherid] [nvarchar](32) default NULL,
	[nodeversiontimestamp] [int] NULL default NULL,
	[nodeversion_user] [nvarchar](255) NULL default NULL,
	[nodeversionlatest] [int] NULL default NULL,
	[nodeversionremoved] [int] NULL default NULL,
	[nodeversion_autoupdate] [tinyint] NOT NULL default 0
) ON [PRIMARY]

GO
CREATE INDEX [contentmap_nodeversion_idx1]
  ON [contentmap_nodeversion] ([obj_id])
GO
CREATE INDEX [contentmap_nodeversion_idx2]
  ON [contentmap_nodeversion] ([id])
GO
CREATE INDEX [contentmap_nodeversion_idx3]
  ON [contentmap_nodeversion] ([obj_type])
GO
CREATE INDEX [contentmap_nodeversion_idx4]
  ON [contentmap_nodeversion] ([motherid])
GO
CREATE INDEX [contentmap_nodeversion_idx5]
  ON [contentmap_nodeversion] ([contentid])
GO
CREATE INDEX [contentmap_nodeversion_idx6]
  ON [contentmap_nodeversion] ([motherid], [contentid])
GO
-- gentics-end-table-contentmap_nodeversion
/****** Object:  Table [dbo].[contentobject]    Script Date: 01/16/2007 13:55:58 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[contentobject](
	[name] [nvarchar](32) NULL default NULL,
	[type] [int] NOT NULL default 0,
	[id_counter] [int] NOT NULL default 0,
	[exclude_versioning] [int] NOT NULL default 0,
 CONSTRAINT [PK_contentobject] PRIMARY KEY CLUSTERED 
(
	[type] ASC
)WITH (PAD_INDEX  = OFF, IGNORE_DUP_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]

GO
/****** Object:  Table [dbo].[contentstatus]    Script Date: 01/16/2007 13:55:58 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[contentstatus](
	[name] [nvarchar](255) NOT NULL default '',
	[intvalue] [int] NULL default NULL,
	[stringvalue] [ntext] NULL,
 CONSTRAINT [PK_contentstatus] PRIMARY KEY CLUSTERED 
(
	[name] ASC
)WITH (PAD_INDEX  = OFF, IGNORE_DUP_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]

GO
ALTER TABLE [dbo].[contentattribute]  WITH CHECK ADD  CONSTRAINT [FK_contentattribute_contentattribute] FOREIGN KEY([id])
REFERENCES [dbo].[contentattribute] ([id])
GO
ALTER TABLE [dbo].[contentattribute] CHECK CONSTRAINT [FK_contentattribute_contentattribute]
