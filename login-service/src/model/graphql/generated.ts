import { GraphQLClient } from 'graphql-request';
import * as Dom from 'graphql-request/dist/types.dom';
import gql from 'graphql-tag';
export type Maybe<T> = T | null;
export type InputMaybe<T> = Maybe<T>;
export type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
export type MakeOptional<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]?: Maybe<T[SubKey]> };
export type MakeMaybe<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]: Maybe<T[SubKey]> };
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: string;
  String: string;
  Boolean: boolean;
  Int: number;
  Float: number;
  DateTime: any;
  Duration: any;
  JSON: any;
  URL: any;
};

/** Filter used to filter AffectedByIssue */
export type AffectedByIssueFilterInput = {
  /** Filter by affectingIssues */
  affectingIssues?: InputMaybe<IssueListFilterInput>;
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<AffectedByIssueFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<AffectedByIssueFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<AffectedByIssueFilterInput>>;
  /** Filters for AffectedByIssues which are related to a Trackable */
  relatedTo?: InputMaybe<Scalars['ID']>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type AffectedByIssueListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<AffectedByIssueFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<AffectedByIssueFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<AffectedByIssueFilterInput>;
};

/** Defines the order of a AffectedByIssue list */
export type AffectedByIssueOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<AffectedByIssueOrderField>;
};

/** Fields a list of AffectedByIssue can be sorted by */
export enum AffectedByIssueOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** Filter used to filter AggregatedIssue */
export type AggregatedIssueFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<AggregatedIssueFilterInput>>;
  /** Filter by count */
  count?: InputMaybe<IntFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by incomingRelations */
  incomingRelations?: InputMaybe<AggregatedIssueRelationListFilterInput>;
  /** Filter by isOpen */
  isOpen?: InputMaybe<BooleanFilterInput>;
  /** Filter by issues */
  issues?: InputMaybe<IssueListFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<AggregatedIssueFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<AggregatedIssueFilterInput>>;
  /** Filter by outgoingRelations */
  outgoingRelations?: InputMaybe<AggregatedIssueRelationListFilterInput>;
  /** Filters for nodes where the related node match this filter */
  relationPartner?: InputMaybe<RelationPartnerFilterInput>;
  /** Filters for nodes where the related node match this filter */
  type?: InputMaybe<IssueTypeFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type AggregatedIssueListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<AggregatedIssueFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<AggregatedIssueFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<AggregatedIssueFilterInput>;
};

/** Defines the order of a AggregatedIssue list */
export type AggregatedIssueOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<AggregatedIssueOrderField>;
};

/** Fields a list of AggregatedIssue can be sorted by */
export enum AggregatedIssueOrderField {
  /** Order by count */
  Count = 'COUNT',
  /** Order by id */
  Id = 'ID'
}

/** Filter used to filter AggregatedIssueRelation */
export type AggregatedIssueRelationFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<AggregatedIssueRelationFilterInput>>;
  /** Filters for nodes where the related node match this filter */
  end?: InputMaybe<AggregatedIssueFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by issueRelations */
  issueRelations?: InputMaybe<IssueRelationListFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<AggregatedIssueRelationFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<AggregatedIssueRelationFilterInput>>;
  /** Filters for nodes where the related node match this filter */
  start?: InputMaybe<AggregatedIssueFilterInput>;
  /** Filters for nodes where the related node match this filter */
  type?: InputMaybe<IssueRelationTypeFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type AggregatedIssueRelationListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<AggregatedIssueRelationFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<AggregatedIssueRelationFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<AggregatedIssueRelationFilterInput>;
};

/** Defines the order of a AggregatedIssueRelation list */
export type AggregatedIssueRelationOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<AggregatedIssueRelationOrderField>;
};

/** Fields a list of AggregatedIssueRelation can be sorted by */
export enum AggregatedIssueRelationOrderField {
  /** Order by id */
  Id = 'ID'
}

/** Non global permission entries */
export enum AllPermissionEntry {
  /**
   * Allows to add the Component to Projects
   * Note: this should be handled very carefully, as adding a Component to a Project gives
   * all users with READ access to the Project READ access to the Component
   */
  AddToProjects = 'ADD_TO_PROJECTS',
  /** Grants all other permissions on the Node except READ. */
  Admin = 'ADMIN',
  /**
   * Allows affecting entities part of this Trackable with any Issues.
   * Affectable entitites include
   *   - the Trackable itself
   *   - in case the Trackable is a Component
   *     - InterfaceSpecifications, their InterfaceSpecificationVersions and their InterfaceParts of the Component (not inherited ones)
   *     - Interfaces on the Component
   *     - ComponentVersions of the Component
   */
  AffectEntitiesWithIssues = 'AFFECT_ENTITIES_WITH_ISSUES',
  /**
   * Allows to create Comments on Issues on this Trackable.
   * Also allows editing of your own Comments.
   */
  Comment = 'COMMENT',
  /**
   * Allows to create new Issues on the Trackable.
   * This includes adding Issues from other Trackables.
   */
  CreateIssues = 'CREATE_ISSUES',
  /** Allows adding Issues on this Trackable to other Trackables. */
  ExportIssues = 'EXPORT_ISSUES',
  /** Allows adding Labels on this Trackable to other Trackables. */
  ExportLabels = 'EXPORT_LABELS',
  /** Allows to add, remove, and update Artefacts on this Trackable. */
  ManageArtefacts = 'MANAGE_ARTEFACTS',
  /** Allows to add / remove ComponentVersions to / from this Project. */
  ManageComponents = 'MANAGE_COMPONENTS',
  /**
   * Allows to add, remove, and update IMSProjects on this Trackable.
   * Note: for adding, `IMSPermissionEntry.SYNC_TRACKABLES` is required additionally
   */
  ManageIms = 'MANAGE_IMS',
  /**
   * Allows to manage issues.
   * This includes `CREATE_ISSUES` and `COMMENT`.
   * This does NOT include `LINK_TO_ISSUES` and `LINK_FROM_ISSUES`.
   * Additionaly includes
   *   - change the Template
   *   - add / remove Labels
   *   - add / remove Artefacts
   *   - change any field on the Issue (title, startDate, dueDate, ...)
   *   - change templated fields
   * In contrast to `MODERATOR`, this does not allow editing / removing Comments of other users
   */
  ManageIssues = 'MANAGE_ISSUES',
  /**
   * Allows to add, remove, and update Labels on this Trackable.
   * Also allows to delete a Label, but only if it is allowed on all Trackable the Label is on.
   */
  ManageLabels = 'MANAGE_LABELS',
  /**
   * Allows to moderate Issues on this Trackable.
   * This allows everything `MANAGE_ISSUES` allows.
   * Additionally, it allows editing and deleting Comments of other Users
   */
  Moderator = 'MODERATOR',
  /**
   * Allows to read the Node (obtain it via the API) and to read certain related Nodes.
   * See documentation for specific Node for the specific conditions.
   */
  Read = 'READ',
  /**
   * Allows to create Relations with a version of this Component or an Interface of this Component
   * as start.
   * Note: as these Relations cannot cause new Interfaces on this Component, this can be granted
   * more permissively compared to `RELATE_TO_COMPONENT`.
   */
  RelateFromComponent = 'RELATE_FROM_COMPONENT',
  /** Allows to create IMSProjects with this IMS. */
  SyncTrackables = 'SYNC_TRACKABLES'
}

/** Filter used to filter Artefact */
export type ArtefactFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<ArtefactFilterInput>>;
  /** Filter by createdAt */
  createdAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  createdBy?: InputMaybe<UserFilterInput>;
  /** Filter by file */
  file?: InputMaybe<StringFilterInput>;
  /** Filter by from */
  from?: InputMaybe<NullableIntFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by issues */
  issues?: InputMaybe<IssueListFilterInput>;
  /** Filter by lastModifiedAt */
  lastModifiedAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  lastModifiedBy?: InputMaybe<UserFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<ArtefactFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<ArtefactFilterInput>>;
  /** Filter by referencingComments */
  referencingComments?: InputMaybe<IssueCommentListFilterInput>;
  /** Filters for nodes where the related node match this filter */
  template?: InputMaybe<ArtefactTemplateFilterInput>;
  /** Filter for templated fields with matching key and values. Entries are joined by AND */
  templatedFields?: InputMaybe<Array<InputMaybe<JsonFieldInput>>>;
  /** Filter by to */
  to?: InputMaybe<NullableIntFilterInput>;
  /** Filters for nodes where the related node match this filter */
  trackable?: InputMaybe<TrackableFilterInput>;
  /** Filter by version */
  version?: InputMaybe<NullableStringFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type ArtefactListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<ArtefactFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<ArtefactFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<ArtefactFilterInput>;
};

/** Defines the order of a Artefact list */
export type ArtefactOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<ArtefactOrderField>;
};

/** Fields a list of Artefact can be sorted by */
export enum ArtefactOrderField {
  /** Order by createdAt */
  CreatedAt = 'CREATED_AT',
  /** Order by file */
  File = 'FILE',
  /** Order by from */
  From = 'FROM',
  /** Order by id */
  Id = 'ID',
  /** Order by lastModifiedAt */
  LastModifiedAt = 'LAST_MODIFIED_AT',
  /** Order by to */
  To = 'TO',
  /** Order by version */
  Version = 'VERSION'
}

/** Filter used to filter ArtefactTemplate */
export type ArtefactTemplateFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<ArtefactTemplateFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by extendedBy */
  extendedBy?: InputMaybe<ArtefactTemplateListFilterInput>;
  /** Filter by extends */
  extends?: InputMaybe<ArtefactTemplateListFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by isDeprecated */
  isDeprecated?: InputMaybe<BooleanFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<ArtefactTemplateFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<ArtefactTemplateFilterInput>>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type ArtefactTemplateListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<ArtefactTemplateFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<ArtefactTemplateFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<ArtefactTemplateFilterInput>;
};

/** Defines the order of a ArtefactTemplate list */
export type ArtefactTemplateOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<ArtefactTemplateOrderField>;
};

/** Fields a list of ArtefactTemplate can be sorted by */
export enum ArtefactTemplateOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** Filter used to filter Assignment */
export type AssignmentFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<AssignmentFilterInput>>;
  /** Filter by createdAt */
  createdAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  createdBy?: InputMaybe<UserFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filters for nodes where the related node match this filter */
  initialType?: InputMaybe<AssignmentTypeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  issue?: InputMaybe<IssueFilterInput>;
  /** Filter by lastModifiedAt */
  lastModifiedAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  lastModifiedBy?: InputMaybe<UserFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<AssignmentFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<AssignmentFilterInput>>;
  /** Filters for nodes where the related node match this filter */
  parentItem?: InputMaybe<ParentTimelineItemFilterInput>;
  /** Filter for specific timeline items. Entries are joined by OR */
  timelineItemTypes?: InputMaybe<Array<TimelineItemType>>;
  /** Filters for nodes where the related node match this filter */
  type?: InputMaybe<AssignmentTypeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  user?: InputMaybe<UserFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type AssignmentListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<AssignmentFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<AssignmentFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<AssignmentFilterInput>;
};

/** Defines the order of a Assignment list */
export type AssignmentOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<AssignmentOrderField>;
};

/** Fields a list of Assignment can be sorted by */
export enum AssignmentOrderField {
  /** Order by createdAt */
  CreatedAt = 'CREATED_AT',
  /** Order by id */
  Id = 'ID',
  /** Order by lastModifiedAt */
  LastModifiedAt = 'LAST_MODIFIED_AT'
}

/** Filter used to filter AssignmentType */
export type AssignmentTypeFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<AssignmentTypeFilterInput>>;
  /** Filter by assignmentsWithType */
  assignmentsWithType?: InputMaybe<AssignmentListFilterInput>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<AssignmentTypeFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<AssignmentTypeFilterInput>>;
  /** Filter by partOf */
  partOf?: InputMaybe<IssueTemplateListFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type AssignmentTypeListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<AssignmentTypeFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<AssignmentTypeFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<AssignmentTypeFilterInput>;
};

/** Defines the order of a AssignmentType list */
export type AssignmentTypeOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<AssignmentTypeOrderField>;
};

/** Fields a list of AssignmentType can be sorted by */
export enum AssignmentTypeOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** Filter used to filter AuditedNode */
export type AuditedNodeFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<AuditedNodeFilterInput>>;
  /** Filter by createdAt */
  createdAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  createdBy?: InputMaybe<UserFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by lastModifiedAt */
  lastModifiedAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  lastModifiedBy?: InputMaybe<UserFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<AuditedNodeFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<AuditedNodeFilterInput>>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type AuditedNodeListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<AuditedNodeFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<AuditedNodeFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<AuditedNodeFilterInput>;
};

/** Defines the order of a AuditedNode list */
export type AuditedNodeOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<AuditedNodeOrderField>;
};

/** Fields a list of AuditedNode can be sorted by */
export enum AuditedNodeOrderField {
  /** Order by createdAt */
  CreatedAt = 'CREATED_AT',
  /** Order by id */
  Id = 'ID',
  /** Order by lastModifiedAt */
  LastModifiedAt = 'LAST_MODIFIED_AT'
}

/** Filter used to filter BasePermission */
export type BasePermissionFilterInput = {
  /** Filter by allUsers */
  allUsers?: InputMaybe<BooleanFilterInput>;
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<BasePermissionFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<BasePermissionFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<BasePermissionFilterInput>>;
  /** Filter by users */
  users?: InputMaybe<GropiusUserListFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type BasePermissionListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<BasePermissionFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<BasePermissionFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<BasePermissionFilterInput>;
};

/** Defines the order of a BasePermission list */
export type BasePermissionOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<BasePermissionOrderField>;
};

/** Fields a list of BasePermission can be sorted by */
export enum BasePermissionOrderField {
  /** Order by allUsers */
  AllUsers = 'ALL_USERS',
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** Filter used to filter Body */
export type BodyFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<BodyFilterInput>>;
  /** Filter by answeredBy */
  answeredBy?: InputMaybe<IssueCommentListFilterInput>;
  /** Filter by bodyLastEditedAt */
  bodyLastEditedAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  bodyLastEditedBy?: InputMaybe<UserFilterInput>;
  /** Filter by createdAt */
  createdAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  createdBy?: InputMaybe<UserFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filters for nodes where the related node match this filter */
  issue?: InputMaybe<IssueFilterInput>;
  /** Filter by lastModifiedAt */
  lastModifiedAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  lastModifiedBy?: InputMaybe<UserFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<BodyFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<BodyFilterInput>>;
  /** Filters for nodes where the related node match this filter */
  parentItem?: InputMaybe<ParentTimelineItemFilterInput>;
  /** Filter for specific timeline items. Entries are joined by OR */
  timelineItemTypes?: InputMaybe<Array<TimelineItemType>>;
};

/** Filter which can be used to filter for Nodes with a specific Boolean field */
export type BooleanFilterInput = {
  /** Matches values which are equal to the provided value */
  eq?: InputMaybe<Scalars['Boolean']>;
  /** Matches values which are equal to any of the provided values */
  in?: InputMaybe<Array<Scalars['Boolean']>>;
};

/** Filter used to filter Comment */
export type CommentFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<CommentFilterInput>>;
  /** Filter by answeredBy */
  answeredBy?: InputMaybe<IssueCommentListFilterInput>;
  /** Filter by bodyLastEditedAt */
  bodyLastEditedAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  bodyLastEditedBy?: InputMaybe<UserFilterInput>;
  /** Filter by createdAt */
  createdAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  createdBy?: InputMaybe<UserFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filters for nodes where the related node match this filter */
  issue?: InputMaybe<IssueFilterInput>;
  /** Filter by lastModifiedAt */
  lastModifiedAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  lastModifiedBy?: InputMaybe<UserFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<CommentFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<CommentFilterInput>>;
  /** Filters for nodes where the related node match this filter */
  parentItem?: InputMaybe<ParentTimelineItemFilterInput>;
  /** Filter for specific timeline items. Entries are joined by OR */
  timelineItemTypes?: InputMaybe<Array<TimelineItemType>>;
};

/** Filter used to filter Component */
export type ComponentFilterInput = {
  /** Filter by affectingIssues */
  affectingIssues?: InputMaybe<IssueListFilterInput>;
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<ComponentFilterInput>>;
  /** Filter by artefacts */
  artefacts?: InputMaybe<ArtefactListFilterInput>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by interfaceSpecifications */
  interfaceSpecifications?: InputMaybe<InterfaceSpecificationListFilterInput>;
  /** Filter by issues */
  issues?: InputMaybe<IssueListFilterInput>;
  /** Filter by labels */
  labels?: InputMaybe<LabelListFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<ComponentFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<ComponentFilterInput>>;
  /** Filter by permissions */
  permissions?: InputMaybe<ComponentPermissionListFilterInput>;
  /** Filter by pinnedIssues */
  pinnedIssues?: InputMaybe<IssueListFilterInput>;
  /** Filters for AffectedByIssues which are related to a Trackable */
  relatedTo?: InputMaybe<Scalars['ID']>;
  /** Filter by repositoryURL */
  repositoryURL?: InputMaybe<NullableStringFilterInput>;
  /** Filter by syncsTo */
  syncsTo?: InputMaybe<ImsProjectListFilterInput>;
  /** Filters for nodes where the related node match this filter */
  template?: InputMaybe<ComponentTemplateFilterInput>;
  /** Filter for templated fields with matching key and values. Entries are joined by AND */
  templatedFields?: InputMaybe<Array<InputMaybe<JsonFieldInput>>>;
  /** Filter by versions */
  versions?: InputMaybe<ComponentVersionListFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type ComponentListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<ComponentFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<ComponentFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<ComponentFilterInput>;
};

/** Defines the order of a Component list */
export type ComponentOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<ComponentOrderField>;
};

/** Fields a list of Component can be sorted by */
export enum ComponentOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** ComponentPermission entry enum type. */
export enum ComponentPermissionEntry {
  /**
   * Allows to add the Component to Projects
   * Note: this should be handled very carefully, as adding a Component to a Project gives
   * all users with READ access to the Project READ access to the Component
   */
  AddToProjects = 'ADD_TO_PROJECTS',
  /** Grants all other permissions on the Node except READ. */
  Admin = 'ADMIN',
  /**
   * Allows affecting entities part of this Trackable with any Issues.
   * Affectable entitites include
   *   - the Trackable itself
   *   - in case the Trackable is a Component
   *     - InterfaceSpecifications, their InterfaceSpecificationVersions and their InterfaceParts of the Component (not inherited ones)
   *     - Interfaces on the Component
   *     - ComponentVersions of the Component
   */
  AffectEntitiesWithIssues = 'AFFECT_ENTITIES_WITH_ISSUES',
  /**
   * Allows to create Comments on Issues on this Trackable.
   * Also allows editing of your own Comments.
   */
  Comment = 'COMMENT',
  /**
   * Allows to create new Issues on the Trackable.
   * This includes adding Issues from other Trackables.
   */
  CreateIssues = 'CREATE_ISSUES',
  /** Allows adding Issues on this Trackable to other Trackables. */
  ExportIssues = 'EXPORT_ISSUES',
  /** Allows adding Labels on this Trackable to other Trackables. */
  ExportLabels = 'EXPORT_LABELS',
  /** Allows to add, remove, and update Artefacts on this Trackable. */
  ManageArtefacts = 'MANAGE_ARTEFACTS',
  /**
   * Allows to add, remove, and update IMSProjects on this Trackable.
   * Note: for adding, `IMSPermissionEntry.SYNC_TRACKABLES` is required additionally
   */
  ManageIms = 'MANAGE_IMS',
  /**
   * Allows to manage issues.
   * This includes `CREATE_ISSUES` and `COMMENT`.
   * This does NOT include `LINK_TO_ISSUES` and `LINK_FROM_ISSUES`.
   * Additionaly includes
   *   - change the Template
   *   - add / remove Labels
   *   - add / remove Artefacts
   *   - change any field on the Issue (title, startDate, dueDate, ...)
   *   - change templated fields
   * In contrast to `MODERATOR`, this does not allow editing / removing Comments of other users
   */
  ManageIssues = 'MANAGE_ISSUES',
  /**
   * Allows to add, remove, and update Labels on this Trackable.
   * Also allows to delete a Label, but only if it is allowed on all Trackable the Label is on.
   */
  ManageLabels = 'MANAGE_LABELS',
  /**
   * Allows to moderate Issues on this Trackable.
   * This allows everything `MANAGE_ISSUES` allows.
   * Additionally, it allows editing and deleting Comments of other Users
   */
  Moderator = 'MODERATOR',
  /**
   * Allows to read the Node (obtain it via the API) and to read certain related Nodes.
   * See documentation for specific Node for the specific conditions.
   */
  Read = 'READ',
  /**
   * Allows to create Relations with a version of this Component or an Interface of this Component
   * as start.
   * Note: as these Relations cannot cause new Interfaces on this Component, this can be granted
   * more permissively compared to `RELATE_TO_COMPONENT`.
   */
  RelateFromComponent = 'RELATE_FROM_COMPONENT'
}

/** Filter used to filter ComponentPermission */
export type ComponentPermissionFilterInput = {
  /** Filter by allUsers */
  allUsers?: InputMaybe<BooleanFilterInput>;
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<ComponentPermissionFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Filter by nodesWithPermission */
  nodesWithPermission?: InputMaybe<ComponentListFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<ComponentPermissionFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<ComponentPermissionFilterInput>>;
  /** Filter by users */
  users?: InputMaybe<GropiusUserListFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type ComponentPermissionListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<ComponentPermissionFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<ComponentPermissionFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<ComponentPermissionFilterInput>;
};

/** Defines the order of a ComponentPermission list */
export type ComponentPermissionOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<ComponentPermissionOrderField>;
};

/** Fields a list of ComponentPermission can be sorted by */
export enum ComponentPermissionOrderField {
  /** Order by allUsers */
  AllUsers = 'ALL_USERS',
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** Filter used to filter ComponentTemplate */
export type ComponentTemplateFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<ComponentTemplateFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by extendedBy */
  extendedBy?: InputMaybe<ComponentTemplateListFilterInput>;
  /** Filter by extends */
  extends?: InputMaybe<ComponentTemplateListFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by isDeprecated */
  isDeprecated?: InputMaybe<BooleanFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<ComponentTemplateFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<ComponentTemplateFilterInput>>;
  /** Filter by possibleEndOfRelations */
  possibleEndOfRelations?: InputMaybe<RelationConditionListFilterInput>;
  /** Filter by possibleInvisibleInterfaceSpecifications */
  possibleInvisibleInterfaceSpecifications?: InputMaybe<InterfaceSpecificationTemplateListFilterInput>;
  /** Filter by possibleStartOfRelations */
  possibleStartOfRelations?: InputMaybe<RelationConditionListFilterInput>;
  /** Filter by possibleVisibleInterfaceSpecifications */
  possibleVisibleInterfaceSpecifications?: InputMaybe<InterfaceSpecificationTemplateListFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type ComponentTemplateListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<ComponentTemplateFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<ComponentTemplateFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<ComponentTemplateFilterInput>;
};

/** Defines the order of a ComponentTemplate list */
export type ComponentTemplateOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<ComponentTemplateOrderField>;
};

/** Fields a list of ComponentTemplate can be sorted by */
export enum ComponentTemplateOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** Filter used to filter ComponentVersion */
export type ComponentVersionFilterInput = {
  /** Filter by affectingIssues */
  affectingIssues?: InputMaybe<IssueListFilterInput>;
  /** Filter by aggregatedIssues */
  aggregatedIssues?: InputMaybe<AggregatedIssueListFilterInput>;
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<ComponentVersionFilterInput>>;
  /** Filters for nodes where the related node match this filter */
  component?: InputMaybe<ComponentFilterInput>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by includingProjects */
  includingProjects?: InputMaybe<ProjectListFilterInput>;
  /** Filter by incomingRelations */
  incomingRelations?: InputMaybe<RelationListFilterInput>;
  /** Filter by interfaceDefinitions */
  interfaceDefinitions?: InputMaybe<InterfaceDefinitionListFilterInput>;
  /** Filter by intraComponentDependencySpecifications */
  intraComponentDependencySpecifications?: InputMaybe<IntraComponentDependencySpecificationListFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<ComponentVersionFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<ComponentVersionFilterInput>>;
  /** Filter by outgoingRelations */
  outgoingRelations?: InputMaybe<RelationListFilterInput>;
  /** Filters for RelationPartners which are part of a Project's component graph */
  partOfProject?: InputMaybe<Scalars['ID']>;
  /** Filters for AffectedByIssues which are related to a Trackable */
  relatedTo?: InputMaybe<Scalars['ID']>;
  /** Filters for nodes where the related node match this filter */
  template?: InputMaybe<ComponentVersionTemplateFilterInput>;
  /** Filter for templated fields with matching key and values. Entries are joined by AND */
  templatedFields?: InputMaybe<Array<InputMaybe<JsonFieldInput>>>;
  /** Filter by version */
  version?: InputMaybe<StringFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type ComponentVersionListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<ComponentVersionFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<ComponentVersionFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<ComponentVersionFilterInput>;
};

/** Defines the order of a ComponentVersion list */
export type ComponentVersionOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<ComponentVersionOrderField>;
};

/** Fields a list of ComponentVersion can be sorted by */
export enum ComponentVersionOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME',
  /** Order by version */
  Version = 'VERSION'
}

/** Filter used to filter ComponentVersionTemplate */
export type ComponentVersionTemplateFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<ComponentVersionTemplateFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<ComponentVersionTemplateFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<ComponentVersionTemplateFilterInput>>;
};

/** Input for the createGropiusUser mutation */
export type CreateGropiusUserInput = {
  /** The avatar of the created GropiusUser */
  avatar?: InputMaybe<Scalars['URL']>;
  /** The displayName of the created User */
  displayName: Scalars['String'];
  /** The email of the created User if present */
  email?: InputMaybe<Scalars['String']>;
  /** If true, the created GropiusUser is a global admin */
  isAdmin: Scalars['Boolean'];
  /** The username of the created GropiusUser, must be unique, must match /^[a-zA-Z0-9_-]+$/ */
  username: Scalars['String'];
};

/** Input for the createIMSUser mutation */
export type CreateImsUserInput = {
  /** The displayName of the created User */
  displayName: Scalars['String'];
  /** The email of the created User if present */
  email?: InputMaybe<Scalars['String']>;
  /** If present, the id of the GropiusUser the created IMSUser is associated with */
  gropiusUser?: InputMaybe<Scalars['ID']>;
  /** The id of the IMS the created IMSUser is part of */
  ims: Scalars['ID'];
  /** Initial values for all templatedFields */
  templatedFields: Array<JsonFieldInput>;
  /** The username of the created IMSUser, must be unique */
  username?: InputMaybe<Scalars['String']>;
};

/** Filter which can be used to filter for Nodes with a specific DateTime field */
export type DateTimeFilterInput = {
  /** Matches values which are equal to the provided value */
  eq?: InputMaybe<Scalars['DateTime']>;
  /** Matches values which are greater than the provided value */
  gt?: InputMaybe<Scalars['DateTime']>;
  /** Matches values which are greater than or equal to the provided value */
  gte?: InputMaybe<Scalars['DateTime']>;
  /** Matches values which are equal to any of the provided values */
  in?: InputMaybe<Array<Scalars['DateTime']>>;
  /** Matches values which are lesser than the provided value */
  lt?: InputMaybe<Scalars['DateTime']>;
  /** Matches values which are lesser than or equal to the provided value */
  lte?: InputMaybe<Scalars['DateTime']>;
};

/** Filter which can be used to filter for Nodes with a specific Float field */
export type FloatFilterInput = {
  /** Matches values which are equal to the provided value */
  eq?: InputMaybe<Scalars['Float']>;
  /** Matches values which are greater than the provided value */
  gt?: InputMaybe<Scalars['Float']>;
  /** Matches values which are greater than or equal to the provided value */
  gte?: InputMaybe<Scalars['Float']>;
  /** Matches values which are equal to any of the provided values */
  in?: InputMaybe<Array<Scalars['Float']>>;
  /** Matches values which are lesser than the provided value */
  lt?: InputMaybe<Scalars['Float']>;
  /** Matches values which are lesser than or equal to the provided value */
  lte?: InputMaybe<Scalars['Float']>;
};

/** Filter used to filter GlobalPermission */
export type GlobalPermissionFilterInput = {
  /** Filter by allUsers */
  allUsers?: InputMaybe<BooleanFilterInput>;
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<GlobalPermissionFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<GlobalPermissionFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<GlobalPermissionFilterInput>>;
  /** Filter by users */
  users?: InputMaybe<GropiusUserListFilterInput>;
};

/** Defines the order of a GlobalPermission list */
export type GlobalPermissionOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<GlobalPermissionOrderField>;
};

/** Fields a list of GlobalPermission can be sorted by */
export enum GlobalPermissionOrderField {
  /** Order by allUsers */
  AllUsers = 'ALL_USERS',
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** Filter used to filter GropiusUser */
export type GropiusUserFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<GropiusUserFilterInput>>;
  /** Filter by assignments */
  assignments?: InputMaybe<AssignmentListFilterInput>;
  /** Filter by canSyncOthers */
  canSyncOthers?: InputMaybe<SyncPermissionTargetListFilterInput>;
  /** Filter by canSyncSelf */
  canSyncSelf?: InputMaybe<SyncPermissionTargetListFilterInput>;
  /** Filter by createdNodes */
  createdNodes?: InputMaybe<AuditedNodeListFilterInput>;
  /** Filter by displayName */
  displayName?: InputMaybe<StringFilterInput>;
  /** Filter by email */
  email?: InputMaybe<NullableStringFilterInput>;
  /** Filter for users with a specific permission on a node */
  hasNodePermission?: InputMaybe<NodePermissionFilterEntry>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by imsUsers */
  imsUsers?: InputMaybe<ImsUserListFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<GropiusUserFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<GropiusUserFilterInput>>;
  /** Filter by participatedIssues */
  participatedIssues?: InputMaybe<IssueListFilterInput>;
  /** Filter by permissions */
  permissions?: InputMaybe<BasePermissionListFilterInput>;
  /** Filter by username */
  username?: InputMaybe<NullableStringFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type GropiusUserListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<GropiusUserFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<GropiusUserFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<GropiusUserFilterInput>;
};

/** Defines the order of a GropiusUser list */
export type GropiusUserOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<GropiusUserOrderField>;
};

/** Fields a list of GropiusUser can be sorted by */
export enum GropiusUserOrderField {
  /** Order by displayName */
  DisplayName = 'DISPLAY_NAME',
  /** Order by email */
  Email = 'EMAIL',
  /** Order by id */
  Id = 'ID',
  /** Order by username */
  Username = 'USERNAME'
}

/** Filter which can be used to filter for Nodes with a specific ID field */
export type IdFilterInput = {
  /** Matches values which are equal to the provided value */
  eq?: InputMaybe<Scalars['ID']>;
  /** Matches values which are equal to any of the provided values */
  in?: InputMaybe<Array<Scalars['ID']>>;
};

/** Filter used to filter IMS */
export type ImsFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<ImsFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<ImsFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<ImsFilterInput>>;
  /** Filter by permissions */
  permissions?: InputMaybe<ImsPermissionListFilterInput>;
  /** Filter by projects */
  projects?: InputMaybe<ImsProjectListFilterInput>;
  /** Filter by syncOthersAllowedBy */
  syncOthersAllowedBy?: InputMaybe<GropiusUserListFilterInput>;
  /** Filter by syncSelfAllowedBy */
  syncSelfAllowedBy?: InputMaybe<GropiusUserListFilterInput>;
  /** Filters for nodes where the related node match this filter */
  template?: InputMaybe<ImsTemplateFilterInput>;
  /** Filter for templated fields with matching key and values. Entries are joined by AND */
  templatedFields?: InputMaybe<Array<InputMaybe<JsonFieldInput>>>;
  /** Filter by users */
  users?: InputMaybe<ImsUserListFilterInput>;
};

/** Filter used to filter IMSIssue */
export type ImsIssueFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<ImsIssueFilterInput>>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filters for nodes where the related node match this filter */
  imsProject?: InputMaybe<ImsProjectFilterInput>;
  /** Filters for nodes where the related node match this filter */
  issue?: InputMaybe<IssueFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<ImsIssueFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<ImsIssueFilterInput>>;
  /** Filters for nodes where the related node match this filter */
  template?: InputMaybe<ImsIssueTemplateFilterInput>;
  /** Filter for templated fields with matching key and values. Entries are joined by AND */
  templatedFields?: InputMaybe<Array<InputMaybe<JsonFieldInput>>>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type ImsIssueListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<ImsIssueFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<ImsIssueFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<ImsIssueFilterInput>;
};

/** Defines the order of a IMSIssue list */
export type ImsIssueOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<ImsIssueOrderField>;
};

/** Fields a list of IMSIssue can be sorted by */
export enum ImsIssueOrderField {
  /** Order by id */
  Id = 'ID'
}

/** Filter used to filter IMSIssueTemplate */
export type ImsIssueTemplateFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<ImsIssueTemplateFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<ImsIssueTemplateFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<ImsIssueTemplateFilterInput>>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type ImsListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<ImsFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<ImsFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<ImsFilterInput>;
};

/** Defines the order of a IMS list */
export type ImsOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<ImsOrderField>;
};

/** Fields a list of IMS can be sorted by */
export enum ImsOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** IMSPermission entry enum type. */
export enum ImsPermissionEntry {
  /** Grants all other permissions on the Node except READ. */
  Admin = 'ADMIN',
  /**
   * Allows to read the Node (obtain it via the API) and to read certain related Nodes.
   * See documentation for specific Node for the specific conditions.
   */
  Read = 'READ',
  /** Allows to create IMSProjects with this IMS. */
  SyncTrackables = 'SYNC_TRACKABLES'
}

/** Filter used to filter IMSPermission */
export type ImsPermissionFilterInput = {
  /** Filter by allUsers */
  allUsers?: InputMaybe<BooleanFilterInput>;
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<ImsPermissionFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Filter by nodesWithPermission */
  nodesWithPermission?: InputMaybe<ImsListFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<ImsPermissionFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<ImsPermissionFilterInput>>;
  /** Filter by users */
  users?: InputMaybe<GropiusUserListFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type ImsPermissionListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<ImsPermissionFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<ImsPermissionFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<ImsPermissionFilterInput>;
};

/** Defines the order of a IMSPermission list */
export type ImsPermissionOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<ImsPermissionOrderField>;
};

/** Fields a list of IMSPermission can be sorted by */
export enum ImsPermissionOrderField {
  /** Order by allUsers */
  AllUsers = 'ALL_USERS',
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** Filter used to filter IMSProject */
export type ImsProjectFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<ImsProjectFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filters for nodes where the related node match this filter */
  ims?: InputMaybe<ImsFilterInput>;
  /** Filter by imsIssues */
  imsIssues?: InputMaybe<ImsIssueListFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<ImsProjectFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<ImsProjectFilterInput>>;
  /** Filter by syncOthersAllowedBy */
  syncOthersAllowedBy?: InputMaybe<GropiusUserListFilterInput>;
  /** Filter by syncSelfAllowedBy */
  syncSelfAllowedBy?: InputMaybe<GropiusUserListFilterInput>;
  /** Filters for nodes where the related node match this filter */
  template?: InputMaybe<ImsProjectTemplateFilterInput>;
  /** Filter for templated fields with matching key and values. Entries are joined by AND */
  templatedFields?: InputMaybe<Array<InputMaybe<JsonFieldInput>>>;
  /** Filters for nodes where the related node match this filter */
  trackable?: InputMaybe<TrackableFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type ImsProjectListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<ImsProjectFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<ImsProjectFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<ImsProjectFilterInput>;
};

/** Defines the order of a IMSProject list */
export type ImsProjectOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<ImsProjectOrderField>;
};

/** Fields a list of IMSProject can be sorted by */
export enum ImsProjectOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** Filter used to filter IMSProjectTemplate */
export type ImsProjectTemplateFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<ImsProjectTemplateFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<ImsProjectTemplateFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<ImsProjectTemplateFilterInput>>;
};

/** Filter used to filter IMSTemplate */
export type ImsTemplateFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<ImsTemplateFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by extendedBy */
  extendedBy?: InputMaybe<ImsTemplateListFilterInput>;
  /** Filter by extends */
  extends?: InputMaybe<ImsTemplateListFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by isDeprecated */
  isDeprecated?: InputMaybe<BooleanFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<ImsTemplateFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<ImsTemplateFilterInput>>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type ImsTemplateListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<ImsTemplateFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<ImsTemplateFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<ImsTemplateFilterInput>;
};

/** Defines the order of a IMSTemplate list */
export type ImsTemplateOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<ImsTemplateOrderField>;
};

/** Fields a list of IMSTemplate can be sorted by */
export enum ImsTemplateOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** Filter used to filter IMSUser */
export type ImsUserFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<ImsUserFilterInput>>;
  /** Filter by assignments */
  assignments?: InputMaybe<AssignmentListFilterInput>;
  /** Filter by createdNodes */
  createdNodes?: InputMaybe<AuditedNodeListFilterInput>;
  /** Filter by displayName */
  displayName?: InputMaybe<StringFilterInput>;
  /** Filter by email */
  email?: InputMaybe<NullableStringFilterInput>;
  /** Filters for nodes where the related node match this filter */
  gropiusUser?: InputMaybe<GropiusUserFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filters for nodes where the related node match this filter */
  ims?: InputMaybe<ImsFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<ImsUserFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<ImsUserFilterInput>>;
  /** Filter by participatedIssues */
  participatedIssues?: InputMaybe<IssueListFilterInput>;
  /** Filters for nodes where the related node match this filter */
  template?: InputMaybe<ImsUserTemplateFilterInput>;
  /** Filter for templated fields with matching key and values. Entries are joined by AND */
  templatedFields?: InputMaybe<Array<InputMaybe<JsonFieldInput>>>;
  /** Filter by username */
  username?: InputMaybe<NullableStringFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type ImsUserListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<ImsUserFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<ImsUserFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<ImsUserFilterInput>;
};

/** Defines the order of a IMSUser list */
export type ImsUserOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<ImsUserOrderField>;
};

/** Fields a list of IMSUser can be sorted by */
export enum ImsUserOrderField {
  /** Order by displayName */
  DisplayName = 'DISPLAY_NAME',
  /** Order by email */
  Email = 'EMAIL',
  /** Order by id */
  Id = 'ID',
  /** Order by username */
  Username = 'USERNAME'
}

/** Filter used to filter IMSUserTemplate */
export type ImsUserTemplateFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<ImsUserTemplateFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<ImsUserTemplateFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<ImsUserTemplateFilterInput>>;
};

/** Filter which can be used to filter for Nodes with a specific Int field */
export type IntFilterInput = {
  /** Matches values which are equal to the provided value */
  eq?: InputMaybe<Scalars['Int']>;
  /** Matches values which are greater than the provided value */
  gt?: InputMaybe<Scalars['Int']>;
  /** Matches values which are greater than or equal to the provided value */
  gte?: InputMaybe<Scalars['Int']>;
  /** Matches values which are equal to any of the provided values */
  in?: InputMaybe<Array<Scalars['Int']>>;
  /** Matches values which are lesser than the provided value */
  lt?: InputMaybe<Scalars['Int']>;
  /** Matches values which are lesser than or equal to the provided value */
  lte?: InputMaybe<Scalars['Int']>;
};

/** Filter used to filter InterfaceDefinition */
export type InterfaceDefinitionFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<InterfaceDefinitionFilterInput>>;
  /** Filters for nodes where the related node match this filter */
  componentVersion?: InputMaybe<ComponentVersionFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filters for nodes where the related node match this filter */
  interfaceSpecificationVersion?: InputMaybe<InterfaceSpecificationVersionFilterInput>;
  /** Filter by invisibleDerivedBy */
  invisibleDerivedBy?: InputMaybe<RelationListFilterInput>;
  /** Filter by invisibleSelfDefined */
  invisibleSelfDefined?: InputMaybe<BooleanFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<InterfaceDefinitionFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<InterfaceDefinitionFilterInput>>;
  /** Filters for nodes where the related node match this filter */
  template?: InputMaybe<InterfaceDefinitionTemplateFilterInput>;
  /** Filter for templated fields with matching key and values. Entries are joined by AND */
  templatedFields?: InputMaybe<Array<InputMaybe<JsonFieldInput>>>;
  /** Filter by visibleDerivedBy */
  visibleDerivedBy?: InputMaybe<RelationListFilterInput>;
  /** Filters for nodes where the related node match this filter */
  visibleInterface?: InputMaybe<InterfaceFilterInput>;
  /** Filter by visibleSelfDefined */
  visibleSelfDefined?: InputMaybe<BooleanFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type InterfaceDefinitionListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<InterfaceDefinitionFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<InterfaceDefinitionFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<InterfaceDefinitionFilterInput>;
};

/** Defines the order of a InterfaceDefinition list */
export type InterfaceDefinitionOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<InterfaceDefinitionOrderField>;
};

/** Fields a list of InterfaceDefinition can be sorted by */
export enum InterfaceDefinitionOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by invisibleSelfDefined */
  InvisibleSelfDefined = 'INVISIBLE_SELF_DEFINED',
  /** Order by visibleSelfDefined */
  VisibleSelfDefined = 'VISIBLE_SELF_DEFINED'
}

/** Filter used to filter InterfaceDefinitionTemplate */
export type InterfaceDefinitionTemplateFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<InterfaceDefinitionTemplateFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<InterfaceDefinitionTemplateFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<InterfaceDefinitionTemplateFilterInput>>;
};

/** Filter used to filter Interface */
export type InterfaceFilterInput = {
  /** Filter by affectingIssues */
  affectingIssues?: InputMaybe<IssueListFilterInput>;
  /** Filter by aggregatedIssues */
  aggregatedIssues?: InputMaybe<AggregatedIssueListFilterInput>;
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<InterfaceFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by incomingRelations */
  incomingRelations?: InputMaybe<RelationListFilterInput>;
  /** Filters for nodes where the related node match this filter */
  interfaceDefinition?: InputMaybe<InterfaceDefinitionFilterInput>;
  /** Filter by intraComponentDependencyParticipants */
  intraComponentDependencyParticipants?: InputMaybe<IntraComponentDependencyParticipantListFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<InterfaceFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<InterfaceFilterInput>>;
  /** Filter by outgoingRelations */
  outgoingRelations?: InputMaybe<RelationListFilterInput>;
  /** Filters for RelationPartners which are part of a Project's component graph */
  partOfProject?: InputMaybe<Scalars['ID']>;
  /** Filters for AffectedByIssues which are related to a Trackable */
  relatedTo?: InputMaybe<Scalars['ID']>;
  /** Filters for nodes where the related node match this filter */
  template?: InputMaybe<InterfaceTemplateFilterInput>;
  /** Filter for templated fields with matching key and values. Entries are joined by AND */
  templatedFields?: InputMaybe<Array<InputMaybe<JsonFieldInput>>>;
};

/** Defines the order of a Interface list */
export type InterfaceOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<InterfaceOrderField>;
};

/** Fields a list of Interface can be sorted by */
export enum InterfaceOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** Filter used to filter InterfacePart */
export type InterfacePartFilterInput = {
  /** Filter by affectingIssues */
  affectingIssues?: InputMaybe<IssueListFilterInput>;
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<InterfacePartFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by includingIncomingRelations */
  includingIncomingRelations?: InputMaybe<RelationListFilterInput>;
  /** Filter by includingIntraComponentDependencyParticipants */
  includingIntraComponentDependencyParticipants?: InputMaybe<IntraComponentDependencyParticipantListFilterInput>;
  /** Filter by includingOutgoingRelations */
  includingOutgoingRelations?: InputMaybe<RelationListFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<InterfacePartFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<InterfacePartFilterInput>>;
  /** Filters for nodes where the related node match this filter */
  partOf?: InputMaybe<InterfaceSpecificationVersionFilterInput>;
  /** Filters for AffectedByIssues which are related to a Trackable */
  relatedTo?: InputMaybe<Scalars['ID']>;
  /** Filters for nodes where the related node match this filter */
  template?: InputMaybe<InterfacePartTemplateFilterInput>;
  /** Filter for templated fields with matching key and values. Entries are joined by AND */
  templatedFields?: InputMaybe<Array<InputMaybe<JsonFieldInput>>>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type InterfacePartListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<InterfacePartFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<InterfacePartFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<InterfacePartFilterInput>;
};

/** Defines the order of a InterfacePart list */
export type InterfacePartOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<InterfacePartOrderField>;
};

/** Fields a list of InterfacePart can be sorted by */
export enum InterfacePartOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** Filter used to filter InterfacePartTemplate */
export type InterfacePartTemplateFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<InterfacePartTemplateFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<InterfacePartTemplateFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<InterfacePartTemplateFilterInput>>;
};

/** Filter used to filter InterfaceSpecificationDerivationCondition */
export type InterfaceSpecificationDerivationConditionFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<InterfaceSpecificationDerivationConditionFilterInput>>;
  /** Filter by derivableInterfaceSpecifications */
  derivableInterfaceSpecifications?: InputMaybe<InterfaceSpecificationTemplateListFilterInput>;
  /** Filter by derivesInvisibleDerived */
  derivesInvisibleDerived?: InputMaybe<BooleanFilterInput>;
  /** Filter by derivesInvisibleSelfDefined */
  derivesInvisibleSelfDefined?: InputMaybe<BooleanFilterInput>;
  /** Filter by derivesVisibleDerived */
  derivesVisibleDerived?: InputMaybe<BooleanFilterInput>;
  /** Filter by derivesVisibleSelfDefined */
  derivesVisibleSelfDefined?: InputMaybe<BooleanFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by isInvisibleDerived */
  isInvisibleDerived?: InputMaybe<BooleanFilterInput>;
  /** Filter by isVisibleDerived */
  isVisibleDerived?: InputMaybe<BooleanFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<InterfaceSpecificationDerivationConditionFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<InterfaceSpecificationDerivationConditionFilterInput>>;
  /** Filters for nodes where the related node match this filter */
  partOf?: InputMaybe<RelationConditionFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type InterfaceSpecificationDerivationConditionListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<InterfaceSpecificationDerivationConditionFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<InterfaceSpecificationDerivationConditionFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<InterfaceSpecificationDerivationConditionFilterInput>;
};

/** Defines the order of a InterfaceSpecificationDerivationCondition list */
export type InterfaceSpecificationDerivationConditionOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<InterfaceSpecificationDerivationConditionOrderField>;
};

/** Fields a list of InterfaceSpecificationDerivationCondition can be sorted by */
export enum InterfaceSpecificationDerivationConditionOrderField {
  /** Order by id */
  Id = 'ID'
}

/** Filter used to filter InterfaceSpecification */
export type InterfaceSpecificationFilterInput = {
  /** Filter by affectingIssues */
  affectingIssues?: InputMaybe<IssueListFilterInput>;
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<InterfaceSpecificationFilterInput>>;
  /** Filters for nodes where the related node match this filter */
  component?: InputMaybe<ComponentFilterInput>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<InterfaceSpecificationFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<InterfaceSpecificationFilterInput>>;
  /** Filters for AffectedByIssues which are related to a Trackable */
  relatedTo?: InputMaybe<Scalars['ID']>;
  /** Filters for nodes where the related node match this filter */
  template?: InputMaybe<InterfaceSpecificationTemplateFilterInput>;
  /** Filter for templated fields with matching key and values. Entries are joined by AND */
  templatedFields?: InputMaybe<Array<InputMaybe<JsonFieldInput>>>;
  /** Filter by versions */
  versions?: InputMaybe<InterfaceSpecificationVersionListFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type InterfaceSpecificationListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<InterfaceSpecificationFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<InterfaceSpecificationFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<InterfaceSpecificationFilterInput>;
};

/** Defines the order of a InterfaceSpecification list */
export type InterfaceSpecificationOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<InterfaceSpecificationOrderField>;
};

/** Fields a list of InterfaceSpecification can be sorted by */
export enum InterfaceSpecificationOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** Filter used to filter InterfaceSpecificationTemplate */
export type InterfaceSpecificationTemplateFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<InterfaceSpecificationTemplateFilterInput>>;
  /** Filter by canBeInvisibleOnComponents */
  canBeInvisibleOnComponents?: InputMaybe<ComponentTemplateListFilterInput>;
  /** Filter by canBeVisibleOnComponents */
  canBeVisibleOnComponents?: InputMaybe<ComponentTemplateListFilterInput>;
  /** Filter by derivableBy */
  derivableBy?: InputMaybe<InterfaceSpecificationDerivationConditionListFilterInput>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by extendedBy */
  extendedBy?: InputMaybe<InterfaceSpecificationTemplateListFilterInput>;
  /** Filter by extends */
  extends?: InputMaybe<InterfaceSpecificationTemplateListFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by isDeprecated */
  isDeprecated?: InputMaybe<BooleanFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<InterfaceSpecificationTemplateFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<InterfaceSpecificationTemplateFilterInput>>;
  /** Filter by possibleEndOfRelations */
  possibleEndOfRelations?: InputMaybe<RelationConditionListFilterInput>;
  /** Filter by possibleStartOfRelations */
  possibleStartOfRelations?: InputMaybe<RelationConditionListFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type InterfaceSpecificationTemplateListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<InterfaceSpecificationTemplateFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<InterfaceSpecificationTemplateFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<InterfaceSpecificationTemplateFilterInput>;
};

/** Defines the order of a InterfaceSpecificationTemplate list */
export type InterfaceSpecificationTemplateOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<InterfaceSpecificationTemplateOrderField>;
};

/** Fields a list of InterfaceSpecificationTemplate can be sorted by */
export enum InterfaceSpecificationTemplateOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** Filter used to filter InterfaceSpecificationVersion */
export type InterfaceSpecificationVersionFilterInput = {
  /** Filter by affectingIssues */
  affectingIssues?: InputMaybe<IssueListFilterInput>;
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<InterfaceSpecificationVersionFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by interfaceDefinitions */
  interfaceDefinitions?: InputMaybe<InterfaceDefinitionListFilterInput>;
  /** Filters for nodes where the related node match this filter */
  interfaceSpecification?: InputMaybe<InterfaceSpecificationFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<InterfaceSpecificationVersionFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<InterfaceSpecificationVersionFilterInput>>;
  /** Filter by parts */
  parts?: InputMaybe<InterfacePartListFilterInput>;
  /** Filters for AffectedByIssues which are related to a Trackable */
  relatedTo?: InputMaybe<Scalars['ID']>;
  /** Filters for nodes where the related node match this filter */
  template?: InputMaybe<InterfaceSpecificationVersionTemplateFilterInput>;
  /** Filter for templated fields with matching key and values. Entries are joined by AND */
  templatedFields?: InputMaybe<Array<InputMaybe<JsonFieldInput>>>;
  /** Filter by version */
  version?: InputMaybe<StringFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type InterfaceSpecificationVersionListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<InterfaceSpecificationVersionFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<InterfaceSpecificationVersionFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<InterfaceSpecificationVersionFilterInput>;
};

/** Defines the order of a InterfaceSpecificationVersion list */
export type InterfaceSpecificationVersionOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<InterfaceSpecificationVersionOrderField>;
};

/** Fields a list of InterfaceSpecificationVersion can be sorted by */
export enum InterfaceSpecificationVersionOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME',
  /** Order by version */
  Version = 'VERSION'
}

/** Filter used to filter InterfaceSpecificationVersionTemplate */
export type InterfaceSpecificationVersionTemplateFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<InterfaceSpecificationVersionTemplateFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<InterfaceSpecificationVersionTemplateFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<InterfaceSpecificationVersionTemplateFilterInput>>;
};

/** Filter used to filter InterfaceTemplate */
export type InterfaceTemplateFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<InterfaceTemplateFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<InterfaceTemplateFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<InterfaceTemplateFilterInput>>;
};

/** Filter used to filter IntraComponentDependencyParticipant */
export type IntraComponentDependencyParticipantFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<IntraComponentDependencyParticipantFilterInput>>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by includedParts */
  includedParts?: InputMaybe<InterfacePartListFilterInput>;
  /** Filters for nodes where the related node match this filter */
  interface?: InputMaybe<InterfaceFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<IntraComponentDependencyParticipantFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<IntraComponentDependencyParticipantFilterInput>>;
  /** Filters for nodes where the related node match this filter */
  usedAsIncomingAt?: InputMaybe<IntraComponentDependencySpecificationFilterInput>;
  /** Filters for nodes where the related node match this filter */
  usedAsOutgoingAt?: InputMaybe<IntraComponentDependencySpecificationFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type IntraComponentDependencyParticipantListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<IntraComponentDependencyParticipantFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<IntraComponentDependencyParticipantFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<IntraComponentDependencyParticipantFilterInput>;
};

/** Defines the order of a IntraComponentDependencyParticipant list */
export type IntraComponentDependencyParticipantOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<IntraComponentDependencyParticipantOrderField>;
};

/** Fields a list of IntraComponentDependencyParticipant can be sorted by */
export enum IntraComponentDependencyParticipantOrderField {
  /** Order by id */
  Id = 'ID'
}

/** Filter used to filter IntraComponentDependencySpecification */
export type IntraComponentDependencySpecificationFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<IntraComponentDependencySpecificationFilterInput>>;
  /** Filters for nodes where the related node match this filter */
  componentVersion?: InputMaybe<ComponentVersionFilterInput>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by incomingParticipants */
  incomingParticipants?: InputMaybe<IntraComponentDependencyParticipantListFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<IntraComponentDependencySpecificationFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<IntraComponentDependencySpecificationFilterInput>>;
  /** Filter by outgoingParticipants */
  outgoingParticipants?: InputMaybe<IntraComponentDependencyParticipantListFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type IntraComponentDependencySpecificationListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<IntraComponentDependencySpecificationFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<IntraComponentDependencySpecificationFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<IntraComponentDependencySpecificationFilterInput>;
};

/** Defines the order of a IntraComponentDependencySpecification list */
export type IntraComponentDependencySpecificationOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<IntraComponentDependencySpecificationOrderField>;
};

/** Fields a list of IntraComponentDependencySpecification can be sorted by */
export enum IntraComponentDependencySpecificationOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** Filter used to filter IssueComment */
export type IssueCommentFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<IssueCommentFilterInput>>;
  /** Filter by answeredBy */
  answeredBy?: InputMaybe<IssueCommentListFilterInput>;
  /** Filters for nodes where the related node match this filter */
  answers?: InputMaybe<CommentFilterInput>;
  /** Filter by bodyLastEditedAt */
  bodyLastEditedAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  bodyLastEditedBy?: InputMaybe<UserFilterInput>;
  /** Filter by createdAt */
  createdAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  createdBy?: InputMaybe<UserFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by isCommentDeleted */
  isCommentDeleted?: InputMaybe<BooleanFilterInput>;
  /** Filters for nodes where the related node match this filter */
  issue?: InputMaybe<IssueFilterInput>;
  /** Filter by lastModifiedAt */
  lastModifiedAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  lastModifiedBy?: InputMaybe<UserFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<IssueCommentFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<IssueCommentFilterInput>>;
  /** Filters for nodes where the related node match this filter */
  parentItem?: InputMaybe<ParentTimelineItemFilterInput>;
  /** Filter by referencedArtefacts */
  referencedArtefacts?: InputMaybe<ArtefactListFilterInput>;
  /** Filter for specific timeline items. Entries are joined by OR */
  timelineItemTypes?: InputMaybe<Array<TimelineItemType>>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type IssueCommentListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<IssueCommentFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<IssueCommentFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<IssueCommentFilterInput>;
};

/** Defines the order of a IssueComment list */
export type IssueCommentOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<IssueCommentOrderField>;
};

/** Fields a list of IssueComment can be sorted by */
export enum IssueCommentOrderField {
  /** Order by bodyLastEditedAt */
  BodyLastEditedAt = 'BODY_LAST_EDITED_AT',
  /** Order by createdAt */
  CreatedAt = 'CREATED_AT',
  /** Order by id */
  Id = 'ID',
  /** Order by lastModifiedAt */
  LastModifiedAt = 'LAST_MODIFIED_AT'
}

/** Filter used to filter Issue */
export type IssueFilterInput = {
  /** Filter by affects */
  affects?: InputMaybe<AffectedByIssueListFilterInput>;
  /** Filter by aggregatedBy */
  aggregatedBy?: InputMaybe<AggregatedIssueListFilterInput>;
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<IssueFilterInput>>;
  /** Filter by artefacts */
  artefacts?: InputMaybe<ArtefactListFilterInput>;
  /** Filter by assignments */
  assignments?: InputMaybe<AssignmentListFilterInput>;
  /** Filters for nodes where the related node match this filter */
  body?: InputMaybe<BodyFilterInput>;
  /** Filter by createdAt */
  createdAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  createdBy?: InputMaybe<UserFilterInput>;
  /** Filter by dueDate */
  dueDate?: InputMaybe<NullableDateTimeFilterInput>;
  /** Filter by estimatedTime */
  estimatedTime?: InputMaybe<NullableDurationFilterInputFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by imsIssues */
  imsIssues?: InputMaybe<ImsIssueListFilterInput>;
  /** Filter by incomingRelations */
  incomingRelations?: InputMaybe<IssueRelationListFilterInput>;
  /** Filter by issueComments */
  issueComments?: InputMaybe<IssueCommentListFilterInput>;
  /** Filter by labels */
  labels?: InputMaybe<LabelListFilterInput>;
  /** Filter by lastModifiedAt */
  lastModifiedAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  lastModifiedBy?: InputMaybe<UserFilterInput>;
  /** Filter by lastUpdatedAt */
  lastUpdatedAt?: InputMaybe<DateTimeFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<IssueFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<IssueFilterInput>>;
  /** Filter by outgoingRelations */
  outgoingRelations?: InputMaybe<IssueRelationListFilterInput>;
  /** Filter by participants */
  participants?: InputMaybe<UserListFilterInput>;
  /** Filter by pinnedOn */
  pinnedOn?: InputMaybe<TrackableListFilterInput>;
  /** Filters for nodes where the related node match this filter */
  priority?: InputMaybe<IssuePriorityFilterInput>;
  /** Filter by spentTime */
  spentTime?: InputMaybe<NullableDurationFilterInputFilterInput>;
  /** Filter by startDate */
  startDate?: InputMaybe<NullableDateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  state?: InputMaybe<IssueStateFilterInput>;
  /** Filters for nodes where the related node match this filter */
  template?: InputMaybe<IssueTemplateFilterInput>;
  /** Filter for templated fields with matching key and values. Entries are joined by AND */
  templatedFields?: InputMaybe<Array<InputMaybe<JsonFieldInput>>>;
  /** Filter by timelineItems */
  timelineItems?: InputMaybe<TimelineItemListFilterInput>;
  /** Filter by title */
  title?: InputMaybe<StringFilterInput>;
  /** Filter by trackables */
  trackables?: InputMaybe<TrackableListFilterInput>;
  /** Filters for nodes where the related node match this filter */
  type?: InputMaybe<IssueTypeFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type IssueListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<IssueFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<IssueFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<IssueFilterInput>;
};

/** Defines the order of a Issue list */
export type IssueOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<IssueOrderField>;
};

/** Fields a list of Issue can be sorted by */
export enum IssueOrderField {
  /** Order by createdAt */
  CreatedAt = 'CREATED_AT',
  /** Order by dueDate */
  DueDate = 'DUE_DATE',
  /** Order by estimatedTime */
  EstimatedTime = 'ESTIMATED_TIME',
  /** Order by id */
  Id = 'ID',
  /** Order by lastModifiedAt */
  LastModifiedAt = 'LAST_MODIFIED_AT',
  /** Order by lastUpdatedAt */
  LastUpdatedAt = 'LAST_UPDATED_AT',
  /** Order by spentTime */
  SpentTime = 'SPENT_TIME',
  /** Order by startDate */
  StartDate = 'START_DATE',
  /** Order by title */
  Title = 'TITLE'
}

/** Filter used to filter IssuePriority */
export type IssuePriorityFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<IssuePriorityFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<IssuePriorityFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<IssuePriorityFilterInput>>;
  /** Filter by partOf */
  partOf?: InputMaybe<IssueTemplateListFilterInput>;
  /** Filter by prioritizedIssues */
  prioritizedIssues?: InputMaybe<IssueListFilterInput>;
  /** Filter by value */
  value?: InputMaybe<FloatFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type IssuePriorityListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<IssuePriorityFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<IssuePriorityFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<IssuePriorityFilterInput>;
};

/** Defines the order of a IssuePriority list */
export type IssuePriorityOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<IssuePriorityOrderField>;
};

/** Fields a list of IssuePriority can be sorted by */
export enum IssuePriorityOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME',
  /** Order by value */
  Value = 'VALUE'
}

/** Filter used to filter IssueRelation */
export type IssueRelationFilterInput = {
  /** Filter by aggregatedBy */
  aggregatedBy?: InputMaybe<AggregatedIssueRelationListFilterInput>;
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<IssueRelationFilterInput>>;
  /** Filter by createdAt */
  createdAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  createdBy?: InputMaybe<UserFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filters for nodes where the related node match this filter */
  initialType?: InputMaybe<IssueRelationTypeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  issue?: InputMaybe<IssueFilterInput>;
  /** Filter by lastModifiedAt */
  lastModifiedAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  lastModifiedBy?: InputMaybe<UserFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<IssueRelationFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<IssueRelationFilterInput>>;
  /** Filters for nodes where the related node match this filter */
  parentItem?: InputMaybe<ParentTimelineItemFilterInput>;
  /** Filters for nodes where the related node match this filter */
  relatedIssue?: InputMaybe<IssueFilterInput>;
  /** Filter for specific timeline items. Entries are joined by OR */
  timelineItemTypes?: InputMaybe<Array<TimelineItemType>>;
  /** Filters for nodes where the related node match this filter */
  type?: InputMaybe<IssueRelationTypeFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type IssueRelationListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<IssueRelationFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<IssueRelationFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<IssueRelationFilterInput>;
};

/** Defines the order of a IssueRelation list */
export type IssueRelationOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<IssueRelationOrderField>;
};

/** Fields a list of IssueRelation can be sorted by */
export enum IssueRelationOrderField {
  /** Order by createdAt */
  CreatedAt = 'CREATED_AT',
  /** Order by id */
  Id = 'ID',
  /** Order by lastModifiedAt */
  LastModifiedAt = 'LAST_MODIFIED_AT'
}

/** Filter used to filter IssueRelationType */
export type IssueRelationTypeFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<IssueRelationTypeFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by inverseName */
  inverseName?: InputMaybe<StringFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<IssueRelationTypeFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<IssueRelationTypeFilterInput>>;
  /** Filter by partOf */
  partOf?: InputMaybe<IssueTemplateListFilterInput>;
  /** Filter by relationsWithType */
  relationsWithType?: InputMaybe<IssueRelationListFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type IssueRelationTypeListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<IssueRelationTypeFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<IssueRelationTypeFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<IssueRelationTypeFilterInput>;
};

/** Defines the order of a IssueRelationType list */
export type IssueRelationTypeOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<IssueRelationTypeOrderField>;
};

/** Fields a list of IssueRelationType can be sorted by */
export enum IssueRelationTypeOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by inverseName */
  InverseName = 'INVERSE_NAME',
  /** Order by name */
  Name = 'NAME'
}

/** Filter used to filter IssueState */
export type IssueStateFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<IssueStateFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by isOpen */
  isOpen?: InputMaybe<BooleanFilterInput>;
  /** Filter by issuesWithState */
  issuesWithState?: InputMaybe<IssueListFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<IssueStateFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<IssueStateFilterInput>>;
  /** Filter by partOf */
  partOf?: InputMaybe<IssueTemplateListFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type IssueStateListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<IssueStateFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<IssueStateFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<IssueStateFilterInput>;
};

/** Defines the order of a IssueState list */
export type IssueStateOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<IssueStateOrderField>;
};

/** Fields a list of IssueState can be sorted by */
export enum IssueStateOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by isOpen */
  IsOpen = 'IS_OPEN',
  /** Order by name */
  Name = 'NAME'
}

/** Filter used to filter IssueTemplate */
export type IssueTemplateFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<IssueTemplateFilterInput>>;
  /** Filter by assignmentTypes */
  assignmentTypes?: InputMaybe<AssignmentTypeListFilterInput>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by extendedBy */
  extendedBy?: InputMaybe<IssueTemplateListFilterInput>;
  /** Filter by extends */
  extends?: InputMaybe<IssueTemplateListFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by isDeprecated */
  isDeprecated?: InputMaybe<BooleanFilterInput>;
  /** Filter by issuePriorities */
  issuePriorities?: InputMaybe<IssuePriorityListFilterInput>;
  /** Filter by issueStates */
  issueStates?: InputMaybe<IssueStateListFilterInput>;
  /** Filter by issueTypes */
  issueTypes?: InputMaybe<IssueTypeListFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<IssueTemplateFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<IssueTemplateFilterInput>>;
  /** Filter by relationTypes */
  relationTypes?: InputMaybe<IssueRelationTypeListFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type IssueTemplateListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<IssueTemplateFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<IssueTemplateFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<IssueTemplateFilterInput>;
};

/** Defines the order of a IssueTemplate list */
export type IssueTemplateOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<IssueTemplateOrderField>;
};

/** Fields a list of IssueTemplate can be sorted by */
export enum IssueTemplateOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** Filter used to filter IssueType */
export type IssueTypeFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<IssueTypeFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by issuesWithType */
  issuesWithType?: InputMaybe<IssueListFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<IssueTypeFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<IssueTypeFilterInput>>;
  /** Filter by partOf */
  partOf?: InputMaybe<IssueTemplateListFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type IssueTypeListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<IssueTypeFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<IssueTypeFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<IssueTypeFilterInput>;
};

/** Defines the order of a IssueType list */
export type IssueTypeOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<IssueTypeOrderField>;
};

/** Fields a list of IssueType can be sorted by */
export enum IssueTypeOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** Input set update the value of a JSON field, like an extension field or a templated field. */
export type JsonFieldInput = {
  /** The name of the field */
  name: Scalars['String'];
  /** The new value of the field */
  value?: InputMaybe<Scalars['JSON']>;
};

/** Filter used to filter Label */
export type LabelFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<LabelFilterInput>>;
  /** Filter by color */
  color?: InputMaybe<StringFilterInput>;
  /** Filter by createdAt */
  createdAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  createdBy?: InputMaybe<UserFilterInput>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by issues */
  issues?: InputMaybe<IssueListFilterInput>;
  /** Filter by lastModifiedAt */
  lastModifiedAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  lastModifiedBy?: InputMaybe<UserFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<LabelFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<LabelFilterInput>>;
  /** Filter by trackables */
  trackables?: InputMaybe<TrackableListFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type LabelListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<LabelFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<LabelFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<LabelFilterInput>;
};

/** Defines the order of a Label list */
export type LabelOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<LabelOrderField>;
};

/** Fields a list of Label can be sorted by */
export enum LabelOrderField {
  /** Order by color */
  Color = 'COLOR',
  /** Order by createdAt */
  CreatedAt = 'CREATED_AT',
  /** Order by id */
  Id = 'ID',
  /** Order by lastModifiedAt */
  LastModifiedAt = 'LAST_MODIFIED_AT',
  /** Order by name */
  Name = 'NAME'
}

/** Type of a Relation marker */
export enum MarkerType {
  /** A regular arrow */
  Arrow = 'ARROW',
  /** A circle */
  Circle = 'CIRCLE',
  /** A diamond */
  Diamond = 'DIAMOND',
  /** A filled circle */
  FilledCircle = 'FILLED_CIRCLE',
  /** A filled diamond */
  FilledDiamond = 'FILLED_DIAMOND',
  /** A filled triangle */
  FilledTriangle = 'FILLED_TRIANGLE',
  /** A triangle */
  Triangle = 'TRIANGLE'
}

export type NodePermissionFilterEntry = {
  /** The node where the user must have the permission */
  node: Scalars['ID'];
  /** The permission the user must have on the node */
  permission: AllPermissionEntry;
};

/** Filter which can be used to filter for Nodes with a specific DateTime field */
export type NullableDateTimeFilterInput = {
  /** Matches values which are equal to the provided value */
  eq?: InputMaybe<Scalars['DateTime']>;
  /** Matches values which are greater than the provided value */
  gt?: InputMaybe<Scalars['DateTime']>;
  /** Matches values which are greater than or equal to the provided value */
  gte?: InputMaybe<Scalars['DateTime']>;
  /** Matches values which are equal to any of the provided values */
  in?: InputMaybe<Array<Scalars['DateTime']>>;
  /** If true, matches only null values, if false, matches only non-null values */
  isNull?: InputMaybe<Scalars['Boolean']>;
  /** Matches values which are lesser than the provided value */
  lt?: InputMaybe<Scalars['DateTime']>;
  /** Matches values which are lesser than or equal to the provided value */
  lte?: InputMaybe<Scalars['DateTime']>;
};

/** Filter which can be used to filter for Nodes with a specific Duration field */
export type NullableDurationFilterInputFilterInput = {
  /** Matches values which are equal to the provided value */
  eq?: InputMaybe<Scalars['Duration']>;
  /** Matches values which are greater than the provided value */
  gt?: InputMaybe<Scalars['Duration']>;
  /** Matches values which are greater than or equal to the provided value */
  gte?: InputMaybe<Scalars['Duration']>;
  /** Matches values which are equal to any of the provided values */
  in?: InputMaybe<Array<Scalars['Duration']>>;
  /** If true, matches only null values, if false, matches only non-null values */
  isNull?: InputMaybe<Scalars['Boolean']>;
  /** Matches values which are lesser than the provided value */
  lt?: InputMaybe<Scalars['Duration']>;
  /** Matches values which are lesser than or equal to the provided value */
  lte?: InputMaybe<Scalars['Duration']>;
};

/** Filter which can be used to filter for Nodes with a specific Int field */
export type NullableIntFilterInput = {
  /** Matches values which are equal to the provided value */
  eq?: InputMaybe<Scalars['Int']>;
  /** Matches values which are greater than the provided value */
  gt?: InputMaybe<Scalars['Int']>;
  /** Matches values which are greater than or equal to the provided value */
  gte?: InputMaybe<Scalars['Int']>;
  /** Matches values which are equal to any of the provided values */
  in?: InputMaybe<Array<Scalars['Int']>>;
  /** If true, matches only null values, if false, matches only non-null values */
  isNull?: InputMaybe<Scalars['Boolean']>;
  /** Matches values which are lesser than the provided value */
  lt?: InputMaybe<Scalars['Int']>;
  /** Matches values which are lesser than or equal to the provided value */
  lte?: InputMaybe<Scalars['Int']>;
};

/** Filter which can be used to filter for Nodes with a specific String field */
export type NullableStringFilterInput = {
  /** Matches Strings which contain the provided value */
  contains?: InputMaybe<Scalars['String']>;
  /** Matches Strings which end with the provided value */
  endsWith?: InputMaybe<Scalars['String']>;
  /** Matches values which are equal to the provided value */
  eq?: InputMaybe<Scalars['String']>;
  /** Matches values which are greater than the provided value */
  gt?: InputMaybe<Scalars['String']>;
  /** Matches values which are greater than or equal to the provided value */
  gte?: InputMaybe<Scalars['String']>;
  /** Matches values which are equal to any of the provided values */
  in?: InputMaybe<Array<Scalars['String']>>;
  /** If true, matches only null values, if false, matches only non-null values */
  isNull?: InputMaybe<Scalars['Boolean']>;
  /** Matches values which are lesser than the provided value */
  lt?: InputMaybe<Scalars['String']>;
  /** Matches values which are lesser than or equal to the provided value */
  lte?: InputMaybe<Scalars['String']>;
  /** Matches Strings using the provided RegEx */
  matches?: InputMaybe<Scalars['String']>;
  /** Matches Strings which start with the provided value */
  startsWith?: InputMaybe<Scalars['String']>;
};

/** Possible direction in which a list of nodes can be ordered */
export enum OrderDirection {
  /** Ascending */
  Asc = 'ASC',
  /** Descending */
  Desc = 'DESC'
}

/** Filter used to filter ParentTimelineItem */
export type ParentTimelineItemFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<ParentTimelineItemFilterInput>>;
  /** Filter by childItems */
  childItems?: InputMaybe<TimelineItemListFilterInput>;
  /** Filter by createdAt */
  createdAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  createdBy?: InputMaybe<UserFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filters for nodes where the related node match this filter */
  issue?: InputMaybe<IssueFilterInput>;
  /** Filter by lastModifiedAt */
  lastModifiedAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  lastModifiedBy?: InputMaybe<UserFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<ParentTimelineItemFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<ParentTimelineItemFilterInput>>;
  /** Filters for nodes where the related node match this filter */
  parentItem?: InputMaybe<ParentTimelineItemFilterInput>;
  /** Filter for specific timeline items. Entries are joined by OR */
  timelineItemTypes?: InputMaybe<Array<TimelineItemType>>;
};

/** Permission entry enum type. */
export enum PermissionEntry {
  /** Allows to create new Components. */
  CanCreateComponents = 'CAN_CREATE_COMPONENTS',
  /** Allows to create new IMSs. */
  CanCreateImss = 'CAN_CREATE_IMSS',
  /** Allows to create new Projects. */
  CanCreateProjects = 'CAN_CREATE_PROJECTS',
  /** Allows to create new Templates. */
  CanCreateTemplates = 'CAN_CREATE_TEMPLATES'
}

/** Filter used to filter Project */
export type ProjectFilterInput = {
  /** Filter by affectingIssues */
  affectingIssues?: InputMaybe<IssueListFilterInput>;
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<ProjectFilterInput>>;
  /** Filter by artefacts */
  artefacts?: InputMaybe<ArtefactListFilterInput>;
  /** Filter by components */
  components?: InputMaybe<ComponentVersionListFilterInput>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by issues */
  issues?: InputMaybe<IssueListFilterInput>;
  /** Filter by labels */
  labels?: InputMaybe<LabelListFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<ProjectFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<ProjectFilterInput>>;
  /** Filter by permissions */
  permissions?: InputMaybe<ProjectPermissionListFilterInput>;
  /** Filter by pinnedIssues */
  pinnedIssues?: InputMaybe<IssueListFilterInput>;
  /** Filters for AffectedByIssues which are related to a Trackable */
  relatedTo?: InputMaybe<Scalars['ID']>;
  /** Filter by repositoryURL */
  repositoryURL?: InputMaybe<NullableStringFilterInput>;
  /** Filter by syncsTo */
  syncsTo?: InputMaybe<ImsProjectListFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type ProjectListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<ProjectFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<ProjectFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<ProjectFilterInput>;
};

/** Defines the order of a Project list */
export type ProjectOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<ProjectOrderField>;
};

/** Fields a list of Project can be sorted by */
export enum ProjectOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** ProjectPermission entry enum type. */
export enum ProjectPermissionEntry {
  /** Grants all other permissions on the Node except READ. */
  Admin = 'ADMIN',
  /**
   * Allows affecting entities part of this Trackable with any Issues.
   * Affectable entitites include
   *   - the Trackable itself
   *   - in case the Trackable is a Component
   *     - InterfaceSpecifications, their InterfaceSpecificationVersions and their InterfaceParts of the Component (not inherited ones)
   *     - Interfaces on the Component
   *     - ComponentVersions of the Component
   */
  AffectEntitiesWithIssues = 'AFFECT_ENTITIES_WITH_ISSUES',
  /**
   * Allows to create Comments on Issues on this Trackable.
   * Also allows editing of your own Comments.
   */
  Comment = 'COMMENT',
  /**
   * Allows to create new Issues on the Trackable.
   * This includes adding Issues from other Trackables.
   */
  CreateIssues = 'CREATE_ISSUES',
  /** Allows adding Issues on this Trackable to other Trackables. */
  ExportIssues = 'EXPORT_ISSUES',
  /** Allows adding Labels on this Trackable to other Trackables. */
  ExportLabels = 'EXPORT_LABELS',
  /** Allows to add, remove, and update Artefacts on this Trackable. */
  ManageArtefacts = 'MANAGE_ARTEFACTS',
  /** Allows to add / remove ComponentVersions to / from this Project. */
  ManageComponents = 'MANAGE_COMPONENTS',
  /**
   * Allows to add, remove, and update IMSProjects on this Trackable.
   * Note: for adding, `IMSPermissionEntry.SYNC_TRACKABLES` is required additionally
   */
  ManageIms = 'MANAGE_IMS',
  /**
   * Allows to manage issues.
   * This includes `CREATE_ISSUES` and `COMMENT`.
   * This does NOT include `LINK_TO_ISSUES` and `LINK_FROM_ISSUES`.
   * Additionaly includes
   *   - change the Template
   *   - add / remove Labels
   *   - add / remove Artefacts
   *   - change any field on the Issue (title, startDate, dueDate, ...)
   *   - change templated fields
   * In contrast to `MODERATOR`, this does not allow editing / removing Comments of other users
   */
  ManageIssues = 'MANAGE_ISSUES',
  /**
   * Allows to add, remove, and update Labels on this Trackable.
   * Also allows to delete a Label, but only if it is allowed on all Trackable the Label is on.
   */
  ManageLabels = 'MANAGE_LABELS',
  /**
   * Allows to moderate Issues on this Trackable.
   * This allows everything `MANAGE_ISSUES` allows.
   * Additionally, it allows editing and deleting Comments of other Users
   */
  Moderator = 'MODERATOR',
  /**
   * Allows to read the Node (obtain it via the API) and to read certain related Nodes.
   * See documentation for specific Node for the specific conditions.
   */
  Read = 'READ'
}

/** Filter used to filter ProjectPermission */
export type ProjectPermissionFilterInput = {
  /** Filter by allUsers */
  allUsers?: InputMaybe<BooleanFilterInput>;
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<ProjectPermissionFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Filter by nodesWithPermission */
  nodesWithPermission?: InputMaybe<ProjectListFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<ProjectPermissionFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<ProjectPermissionFilterInput>>;
  /** Filter by users */
  users?: InputMaybe<GropiusUserListFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type ProjectPermissionListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<ProjectPermissionFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<ProjectPermissionFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<ProjectPermissionFilterInput>;
};

/** Defines the order of a ProjectPermission list */
export type ProjectPermissionOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<ProjectPermissionOrderField>;
};

/** Fields a list of ProjectPermission can be sorted by */
export enum ProjectPermissionOrderField {
  /** Order by allUsers */
  AllUsers = 'ALL_USERS',
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** Filter used to filter RelationCondition */
export type RelationConditionFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<RelationConditionFilterInput>>;
  /** Filter by from */
  from?: InputMaybe<RelationPartnerTemplateListFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by interfaceSpecificationDerivationConditions */
  interfaceSpecificationDerivationConditions?: InputMaybe<InterfaceSpecificationDerivationConditionListFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<RelationConditionFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<RelationConditionFilterInput>>;
  /** Filter by partOf */
  partOf?: InputMaybe<RelationTemplateListFilterInput>;
  /** Filter by to */
  to?: InputMaybe<RelationPartnerTemplateListFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type RelationConditionListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<RelationConditionFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<RelationConditionFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<RelationConditionFilterInput>;
};

/** Defines the order of a RelationCondition list */
export type RelationConditionOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<RelationConditionOrderField>;
};

/** Fields a list of RelationCondition can be sorted by */
export enum RelationConditionOrderField {
  /** Order by id */
  Id = 'ID'
}

/** Filter used to filter Relation */
export type RelationFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<RelationFilterInput>>;
  /** Filter by derivesInvisible */
  derivesInvisible?: InputMaybe<InterfaceDefinitionListFilterInput>;
  /** Filter by derivesVisible */
  derivesVisible?: InputMaybe<InterfaceDefinitionListFilterInput>;
  /** Filters for nodes where the related node match this filter */
  end?: InputMaybe<RelationPartnerFilterInput>;
  /** Filter by endParts */
  endParts?: InputMaybe<InterfacePartListFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<RelationFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<RelationFilterInput>>;
  /** Filters for nodes where the related node match this filter */
  start?: InputMaybe<RelationPartnerFilterInput>;
  /** Filter by startParts */
  startParts?: InputMaybe<InterfacePartListFilterInput>;
  /** Filters for nodes where the related node match this filter */
  template?: InputMaybe<RelationTemplateFilterInput>;
  /** Filter for templated fields with matching key and values. Entries are joined by AND */
  templatedFields?: InputMaybe<Array<InputMaybe<JsonFieldInput>>>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type RelationListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<RelationFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<RelationFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<RelationFilterInput>;
};

/** Defines the order of a Relation list */
export type RelationOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<RelationOrderField>;
};

/** Fields a list of Relation can be sorted by */
export enum RelationOrderField {
  /** Order by id */
  Id = 'ID'
}

/** Filter used to filter RelationPartner */
export type RelationPartnerFilterInput = {
  /** Filter by affectingIssues */
  affectingIssues?: InputMaybe<IssueListFilterInput>;
  /** Filter by aggregatedIssues */
  aggregatedIssues?: InputMaybe<AggregatedIssueListFilterInput>;
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<RelationPartnerFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by incomingRelations */
  incomingRelations?: InputMaybe<RelationListFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<RelationPartnerFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<RelationPartnerFilterInput>>;
  /** Filter by outgoingRelations */
  outgoingRelations?: InputMaybe<RelationListFilterInput>;
  /** Filters for RelationPartners which are part of a Project's component graph */
  partOfProject?: InputMaybe<Scalars['ID']>;
  /** Filters for AffectedByIssues which are related to a Trackable */
  relatedTo?: InputMaybe<Scalars['ID']>;
  /** Filter for templated fields with matching key and values. Entries are joined by AND */
  templatedFields?: InputMaybe<Array<InputMaybe<JsonFieldInput>>>;
};

/** Filter used to filter RelationPartnerTemplate */
export type RelationPartnerTemplateFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<RelationPartnerTemplateFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by isDeprecated */
  isDeprecated?: InputMaybe<BooleanFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<RelationPartnerTemplateFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<RelationPartnerTemplateFilterInput>>;
  /** Filter by possibleEndOfRelations */
  possibleEndOfRelations?: InputMaybe<RelationConditionListFilterInput>;
  /** Filter by possibleStartOfRelations */
  possibleStartOfRelations?: InputMaybe<RelationConditionListFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type RelationPartnerTemplateListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<RelationPartnerTemplateFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<RelationPartnerTemplateFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<RelationPartnerTemplateFilterInput>;
};

/** Defines the order of a RelationPartnerTemplate list */
export type RelationPartnerTemplateOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<RelationPartnerTemplateOrderField>;
};

/** Fields a list of RelationPartnerTemplate can be sorted by */
export enum RelationPartnerTemplateOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** Filter used to filter RelationTemplate */
export type RelationTemplateFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<RelationTemplateFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by extendedBy */
  extendedBy?: InputMaybe<RelationTemplateListFilterInput>;
  /** Filter by extends */
  extends?: InputMaybe<RelationTemplateListFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by isDeprecated */
  isDeprecated?: InputMaybe<BooleanFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<RelationTemplateFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<RelationTemplateFilterInput>>;
  /** Filter by relationConditions */
  relationConditions?: InputMaybe<RelationConditionListFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type RelationTemplateListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<RelationTemplateFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<RelationTemplateFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<RelationTemplateFilterInput>;
};

/** Defines the order of a RelationTemplate list */
export type RelationTemplateOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<RelationTemplateOrderField>;
};

/** Fields a list of RelationTemplate can be sorted by */
export enum RelationTemplateOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** Type of a Shape */
export enum ShapeType {
  /** A Circle */
  Circle = 'CIRCLE',
  /** An Ellipse */
  Ellipse = 'ELLIPSE',
  /** A Hexagon */
  Hexagon = 'HEXAGON',
  /** A Rectangle */
  Rect = 'RECT',
  /** A Rhombus */
  Rhombus = 'RHOMBUS'
}

/** Filter which can be used to filter for Nodes with a specific String field */
export type StringFilterInput = {
  /** Matches Strings which contain the provided value */
  contains?: InputMaybe<Scalars['String']>;
  /** Matches Strings which end with the provided value */
  endsWith?: InputMaybe<Scalars['String']>;
  /** Matches values which are equal to the provided value */
  eq?: InputMaybe<Scalars['String']>;
  /** Matches values which are greater than the provided value */
  gt?: InputMaybe<Scalars['String']>;
  /** Matches values which are greater than or equal to the provided value */
  gte?: InputMaybe<Scalars['String']>;
  /** Matches values which are equal to any of the provided values */
  in?: InputMaybe<Array<Scalars['String']>>;
  /** Matches values which are lesser than the provided value */
  lt?: InputMaybe<Scalars['String']>;
  /** Matches values which are lesser than or equal to the provided value */
  lte?: InputMaybe<Scalars['String']>;
  /** Matches Strings using the provided RegEx */
  matches?: InputMaybe<Scalars['String']>;
  /** Matches Strings which start with the provided value */
  startsWith?: InputMaybe<Scalars['String']>;
};

/** Filter used to filter SyncPermissionTarget */
export type SyncPermissionTargetFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<SyncPermissionTargetFilterInput>>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<SyncPermissionTargetFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<SyncPermissionTargetFilterInput>>;
  /** Filter by syncOthersAllowedBy */
  syncOthersAllowedBy?: InputMaybe<GropiusUserListFilterInput>;
  /** Filter by syncSelfAllowedBy */
  syncSelfAllowedBy?: InputMaybe<GropiusUserListFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type SyncPermissionTargetListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<SyncPermissionTargetFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<SyncPermissionTargetFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<SyncPermissionTargetFilterInput>;
};

/** Defines the order of a SyncPermissionTarget list */
export type SyncPermissionTargetOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<SyncPermissionTargetOrderField>;
};

/** Fields a list of SyncPermissionTarget can be sorted by */
export enum SyncPermissionTargetOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** Filter used to filter TimelineItem */
export type TimelineItemFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<TimelineItemFilterInput>>;
  /** Filter by createdAt */
  createdAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  createdBy?: InputMaybe<UserFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filters for nodes where the related node match this filter */
  issue?: InputMaybe<IssueFilterInput>;
  /** Filter by lastModifiedAt */
  lastModifiedAt?: InputMaybe<DateTimeFilterInput>;
  /** Filters for nodes where the related node match this filter */
  lastModifiedBy?: InputMaybe<UserFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<TimelineItemFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<TimelineItemFilterInput>>;
  /** Filters for nodes where the related node match this filter */
  parentItem?: InputMaybe<ParentTimelineItemFilterInput>;
  /** Filter for specific timeline items. Entries are joined by OR */
  timelineItemTypes?: InputMaybe<Array<TimelineItemType>>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type TimelineItemListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<TimelineItemFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<TimelineItemFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<TimelineItemFilterInput>;
};

/** Defines the order of a TimelineItem list */
export type TimelineItemOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<TimelineItemOrderField>;
};

/** Fields a list of TimelineItem can be sorted by */
export enum TimelineItemOrderField {
  /** Order by createdAt */
  CreatedAt = 'CREATED_AT',
  /** Order by id */
  Id = 'ID',
  /** Order by lastModifiedAt */
  LastModifiedAt = 'LAST_MODIFIED_AT'
}

/** All timeline items types */
export enum TimelineItemType {
  /** AbstractTypeChangedEvent timeline item */
  AbstractTypeChangedEvent = 'ABSTRACT_TYPE_CHANGED_EVENT',
  /** AddedAffectedEntityEvent timeline item */
  AddedAffectedEntityEvent = 'ADDED_AFFECTED_ENTITY_EVENT',
  /** AddedArtefactEvent timeline item */
  AddedArtefactEvent = 'ADDED_ARTEFACT_EVENT',
  /** AddedLabelEvent timeline item */
  AddedLabelEvent = 'ADDED_LABEL_EVENT',
  /** AddedToPinnedIssuesEvent timeline item */
  AddedToPinnedIssuesEvent = 'ADDED_TO_PINNED_ISSUES_EVENT',
  /** AddedToTrackableEvent timeline item */
  AddedToTrackableEvent = 'ADDED_TO_TRACKABLE_EVENT',
  /** Assignment timeline item */
  Assignment = 'ASSIGNMENT',
  /** AssignmentTypeChangedEvent timeline item */
  AssignmentTypeChangedEvent = 'ASSIGNMENT_TYPE_CHANGED_EVENT',
  /** Body timeline item */
  Body = 'BODY',
  /** Comment timeline item */
  Comment = 'COMMENT',
  /** DueDateChangedEvent timeline item */
  DueDateChangedEvent = 'DUE_DATE_CHANGED_EVENT',
  /** EstimatedTimeChangedEvent timeline item */
  EstimatedTimeChangedEvent = 'ESTIMATED_TIME_CHANGED_EVENT',
  /** IncomingRelationTypeChangedEvent timeline item */
  IncomingRelationTypeChangedEvent = 'INCOMING_RELATION_TYPE_CHANGED_EVENT',
  /** IssueComment timeline item */
  IssueComment = 'ISSUE_COMMENT',
  /** IssueRelation timeline item */
  IssueRelation = 'ISSUE_RELATION',
  /** OutgoingRelationTypeChangedEvent timeline item */
  OutgoingRelationTypeChangedEvent = 'OUTGOING_RELATION_TYPE_CHANGED_EVENT',
  /** ParentTimelineItem timeline item */
  ParentTimelineItem = 'PARENT_TIMELINE_ITEM',
  /** PriorityChangedEvent timeline item */
  PriorityChangedEvent = 'PRIORITY_CHANGED_EVENT',
  /** PublicTimelineItem timeline item */
  PublicTimelineItem = 'PUBLIC_TIMELINE_ITEM',
  /** RelatedByIssueEvent timeline item */
  RelatedByIssueEvent = 'RELATED_BY_ISSUE_EVENT',
  /** RelationTypeChangedEvent timeline item */
  RelationTypeChangedEvent = 'RELATION_TYPE_CHANGED_EVENT',
  /** RemovedAffectedEntityEvent timeline item */
  RemovedAffectedEntityEvent = 'REMOVED_AFFECTED_ENTITY_EVENT',
  /** RemovedArtefactEvent timeline item */
  RemovedArtefactEvent = 'REMOVED_ARTEFACT_EVENT',
  /** RemovedAssignmentEvent timeline item */
  RemovedAssignmentEvent = 'REMOVED_ASSIGNMENT_EVENT',
  /** RemovedFromPinnedIssuesEvent timeline item */
  RemovedFromPinnedIssuesEvent = 'REMOVED_FROM_PINNED_ISSUES_EVENT',
  /** RemovedFromTrackableEvent timeline item */
  RemovedFromTrackableEvent = 'REMOVED_FROM_TRACKABLE_EVENT',
  /** RemovedIncomingRelationEvent timeline item */
  RemovedIncomingRelationEvent = 'REMOVED_INCOMING_RELATION_EVENT',
  /** RemovedLabelEvent timeline item */
  RemovedLabelEvent = 'REMOVED_LABEL_EVENT',
  /** RemovedOutgoingRelationEvent timeline item */
  RemovedOutgoingRelationEvent = 'REMOVED_OUTGOING_RELATION_EVENT',
  /** RemovedRelationEvent timeline item */
  RemovedRelationEvent = 'REMOVED_RELATION_EVENT',
  /** RemovedTemplatedFieldEvent timeline item */
  RemovedTemplatedFieldEvent = 'REMOVED_TEMPLATED_FIELD_EVENT',
  /** SpentTimeChangedEvent timeline item */
  SpentTimeChangedEvent = 'SPENT_TIME_CHANGED_EVENT',
  /** StartDateChangedEvent timeline item */
  StartDateChangedEvent = 'START_DATE_CHANGED_EVENT',
  /** StateChangedEvent timeline item */
  StateChangedEvent = 'STATE_CHANGED_EVENT',
  /** TemplatedFieldChangedEvent timeline item */
  TemplatedFieldChangedEvent = 'TEMPLATED_FIELD_CHANGED_EVENT',
  /** TemplateChangedEvent timeline item */
  TemplateChangedEvent = 'TEMPLATE_CHANGED_EVENT',
  /** TimelineItem timeline item */
  TimelineItem = 'TIMELINE_ITEM',
  /** TitleChangedEvent timeline item */
  TitleChangedEvent = 'TITLE_CHANGED_EVENT',
  /** TypeChangedEvent timeline item */
  TypeChangedEvent = 'TYPE_CHANGED_EVENT'
}

/** Filter used to filter Trackable */
export type TrackableFilterInput = {
  /** Filter by affectingIssues */
  affectingIssues?: InputMaybe<IssueListFilterInput>;
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<TrackableFilterInput>>;
  /** Filter by artefacts */
  artefacts?: InputMaybe<ArtefactListFilterInput>;
  /** Filter by description */
  description?: InputMaybe<StringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Filter by issues */
  issues?: InputMaybe<IssueListFilterInput>;
  /** Filter by labels */
  labels?: InputMaybe<LabelListFilterInput>;
  /** Filter by name */
  name?: InputMaybe<StringFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<TrackableFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<TrackableFilterInput>>;
  /** Filter by pinnedIssues */
  pinnedIssues?: InputMaybe<IssueListFilterInput>;
  /** Filters for AffectedByIssues which are related to a Trackable */
  relatedTo?: InputMaybe<Scalars['ID']>;
  /** Filter by repositoryURL */
  repositoryURL?: InputMaybe<NullableStringFilterInput>;
  /** Filter by syncsTo */
  syncsTo?: InputMaybe<ImsProjectListFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type TrackableListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<TrackableFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<TrackableFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<TrackableFilterInput>;
};

/** Defines the order of a Trackable list */
export type TrackableOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<TrackableOrderField>;
};

/** Fields a list of Trackable can be sorted by */
export enum TrackableOrderField {
  /** Order by id */
  Id = 'ID',
  /** Order by name */
  Name = 'NAME'
}

/** Input for the updateIMSUser mutation */
export type UpdateImsUserInput = {
  /** The new displayName of the User to update */
  displayName?: InputMaybe<Scalars['String']>;
  /** The new email of the User to update */
  email?: InputMaybe<Scalars['String']>;
  /**
   * The id of the GropiusUser the updated IMSUser is associated with, replaces existing association
   *         or removes it if null is provided.
   *
   */
  gropiusUser?: InputMaybe<Scalars['ID']>;
  /** The id of the node to update */
  id: Scalars['ID'];
  /** Values for templatedFields to update */
  templatedFields?: InputMaybe<Array<JsonFieldInput>>;
  /** The new username of the updated IMSUser */
  username?: InputMaybe<Scalars['String']>;
};

/** Filter used to filter User */
export type UserFilterInput = {
  /** Connects all subformulas via and */
  and?: InputMaybe<Array<UserFilterInput>>;
  /** Filter by assignments */
  assignments?: InputMaybe<AssignmentListFilterInput>;
  /** Filter by createdNodes */
  createdNodes?: InputMaybe<AuditedNodeListFilterInput>;
  /** Filter by displayName */
  displayName?: InputMaybe<StringFilterInput>;
  /** Filter by email */
  email?: InputMaybe<NullableStringFilterInput>;
  /** Filter by id */
  id?: InputMaybe<IdFilterInput>;
  /** Negates the subformula */
  not?: InputMaybe<UserFilterInput>;
  /** Connects all subformulas via or */
  or?: InputMaybe<Array<UserFilterInput>>;
  /** Filter by participatedIssues */
  participatedIssues?: InputMaybe<IssueListFilterInput>;
  /** Filter by username */
  username?: InputMaybe<NullableStringFilterInput>;
};

/** Used to filter by a connection-based property. Fields are joined by AND */
export type UserListFilterInput = {
  /** Filters for nodes where all of the related nodes match this filter */
  all?: InputMaybe<UserFilterInput>;
  /** Filters for nodes where any of the related nodes match this filter */
  any?: InputMaybe<UserFilterInput>;
  /** Filters for nodes where none of the related nodes match this filter */
  none?: InputMaybe<UserFilterInput>;
};

/** Defines the order of a User list */
export type UserOrder = {
  /** The direction to order by, defaults to ASC */
  direction?: InputMaybe<OrderDirection>;
  /** The field to order by, defaults to ID */
  field?: InputMaybe<UserOrderField>;
};

/** Fields a list of User can be sorted by */
export enum UserOrderField {
  /** Order by displayName */
  DisplayName = 'DISPLAY_NAME',
  /** Order by email */
  Email = 'EMAIL',
  /** Order by id */
  Id = 'ID',
  /** Order by username */
  Username = 'USERNAME'
}

export type ImsUserWithDetailFragment = { __typename: 'IMSUser', id: string, username?: string | null, displayName: string, email?: string | null, templatedFields: Array<{ __typename: 'JSONField', name: string, value?: any | null }>, ims: { __typename: 'IMS', id: string, name: string, description: string, templatedFields: Array<{ __typename: 'JSONField', name: string, value?: any | null }> } };

export type GetBasicImsUserDataQueryVariables = Exact<{
  imsUserId: Scalars['ID'];
}>;


export type GetBasicImsUserDataQuery = { __typename?: 'Query', node?: { __typename: 'AddedAffectedEntityEvent', id: string } | { __typename: 'AddedArtefactEvent', id: string } | { __typename: 'AddedLabelEvent', id: string } | { __typename: 'AddedToPinnedIssuesEvent', id: string } | { __typename: 'AddedToTrackableEvent', id: string } | { __typename: 'AggregatedIssue', id: string } | { __typename: 'AggregatedIssueRelation', id: string } | { __typename: 'Artefact', id: string } | { __typename: 'ArtefactTemplate', id: string } | { __typename: 'Assignment', id: string } | { __typename: 'AssignmentType', id: string } | { __typename: 'AssignmentTypeChangedEvent', id: string } | { __typename: 'Body', id: string } | { __typename: 'Component', id: string } | { __typename: 'ComponentPermission', id: string } | { __typename: 'ComponentTemplate', id: string } | { __typename: 'ComponentVersion', id: string } | { __typename: 'ComponentVersionTemplate', id: string } | { __typename: 'DueDateChangedEvent', id: string } | { __typename: 'EstimatedTimeChangedEvent', id: string } | { __typename: 'FillStyle', id: string } | { __typename: 'GlobalPermission', id: string } | { __typename: 'GropiusUser', id: string } | { __typename: 'IMS', id: string } | { __typename: 'IMSIssue', id: string } | { __typename: 'IMSIssueTemplate', id: string } | { __typename: 'IMSPermission', id: string } | { __typename: 'IMSProject', id: string } | { __typename: 'IMSProjectTemplate', id: string } | { __typename: 'IMSTemplate', id: string } | { __typename: 'IMSUser', id: string } | { __typename: 'IMSUserTemplate', id: string } | { __typename: 'IncomingRelationTypeChangedEvent', id: string } | { __typename: 'Interface', id: string } | { __typename: 'InterfaceDefinition', id: string } | { __typename: 'InterfaceDefinitionTemplate', id: string } | { __typename: 'InterfacePart', id: string } | { __typename: 'InterfacePartTemplate', id: string } | { __typename: 'InterfaceSpecification', id: string } | { __typename: 'InterfaceSpecificationDerivationCondition', id: string } | { __typename: 'InterfaceSpecificationTemplate', id: string } | { __typename: 'InterfaceSpecificationVersion', id: string } | { __typename: 'InterfaceSpecificationVersionTemplate', id: string } | { __typename: 'InterfaceTemplate', id: string } | { __typename: 'IntraComponentDependencyParticipant', id: string } | { __typename: 'IntraComponentDependencySpecification', id: string } | { __typename: 'Issue', id: string } | { __typename: 'IssueComment', id: string } | { __typename: 'IssuePriority', id: string } | { __typename: 'IssueRelation', id: string } | { __typename: 'IssueRelationType', id: string } | { __typename: 'IssueState', id: string } | { __typename: 'IssueTemplate', id: string } | { __typename: 'IssueType', id: string } | { __typename: 'Label', id: string } | { __typename: 'OutgoingRelationTypeChangedEvent', id: string } | { __typename: 'PriorityChangedEvent', id: string } | { __typename: 'Project', id: string } | { __typename: 'ProjectPermission', id: string } | { __typename: 'RelatedByIssueEvent', id: string } | { __typename: 'Relation', id: string } | { __typename: 'RelationCondition', id: string } | { __typename: 'RelationTemplate', id: string } | { __typename: 'RemovedAffectedEntityEvent', id: string } | { __typename: 'RemovedArtefactEvent', id: string } | { __typename: 'RemovedAssignmentEvent', id: string } | { __typename: 'RemovedFromPinnedIssuesEvent', id: string } | { __typename: 'RemovedFromTrackableEvent', id: string } | { __typename: 'RemovedIncomingRelationEvent', id: string } | { __typename: 'RemovedLabelEvent', id: string } | { __typename: 'RemovedOutgoingRelationEvent', id: string } | { __typename: 'RemovedTemplatedFieldEvent', id: string } | { __typename: 'SpentTimeChangedEvent', id: string } | { __typename: 'StartDateChangedEvent', id: string } | { __typename: 'StateChangedEvent', id: string } | { __typename: 'StrokeStyle', id: string } | { __typename: 'TemplateChangedEvent', id: string } | { __typename: 'TemplatedFieldChangedEvent', id: string } | { __typename: 'TitleChangedEvent', id: string } | { __typename: 'TypeChangedEvent', id: string } | null };

export type GetImsUserDetailsQueryVariables = Exact<{
  imsUserId: Scalars['ID'];
}>;


export type GetImsUserDetailsQuery = { __typename?: 'Query', node?: { __typename?: 'AddedAffectedEntityEvent' } | { __typename?: 'AddedArtefactEvent' } | { __typename?: 'AddedLabelEvent' } | { __typename?: 'AddedToPinnedIssuesEvent' } | { __typename?: 'AddedToTrackableEvent' } | { __typename?: 'AggregatedIssue' } | { __typename?: 'AggregatedIssueRelation' } | { __typename?: 'Artefact' } | { __typename?: 'ArtefactTemplate' } | { __typename?: 'Assignment' } | { __typename?: 'AssignmentType' } | { __typename?: 'AssignmentTypeChangedEvent' } | { __typename?: 'Body' } | { __typename?: 'Component' } | { __typename?: 'ComponentPermission' } | { __typename?: 'ComponentTemplate' } | { __typename?: 'ComponentVersion' } | { __typename?: 'ComponentVersionTemplate' } | { __typename?: 'DueDateChangedEvent' } | { __typename?: 'EstimatedTimeChangedEvent' } | { __typename?: 'FillStyle' } | { __typename?: 'GlobalPermission' } | { __typename?: 'GropiusUser' } | { __typename?: 'IMS' } | { __typename?: 'IMSIssue' } | { __typename?: 'IMSIssueTemplate' } | { __typename?: 'IMSPermission' } | { __typename?: 'IMSProject' } | { __typename?: 'IMSProjectTemplate' } | { __typename?: 'IMSTemplate' } | { __typename: 'IMSUser', id: string, username?: string | null, displayName: string, email?: string | null, templatedFields: Array<{ __typename: 'JSONField', name: string, value?: any | null }>, ims: { __typename: 'IMS', id: string, name: string, description: string, templatedFields: Array<{ __typename: 'JSONField', name: string, value?: any | null }> } } | { __typename?: 'IMSUserTemplate' } | { __typename?: 'IncomingRelationTypeChangedEvent' } | { __typename?: 'Interface' } | { __typename?: 'InterfaceDefinition' } | { __typename?: 'InterfaceDefinitionTemplate' } | { __typename?: 'InterfacePart' } | { __typename?: 'InterfacePartTemplate' } | { __typename?: 'InterfaceSpecification' } | { __typename?: 'InterfaceSpecificationDerivationCondition' } | { __typename?: 'InterfaceSpecificationTemplate' } | { __typename?: 'InterfaceSpecificationVersion' } | { __typename?: 'InterfaceSpecificationVersionTemplate' } | { __typename?: 'InterfaceTemplate' } | { __typename?: 'IntraComponentDependencyParticipant' } | { __typename?: 'IntraComponentDependencySpecification' } | { __typename?: 'Issue' } | { __typename?: 'IssueComment' } | { __typename?: 'IssuePriority' } | { __typename?: 'IssueRelation' } | { __typename?: 'IssueRelationType' } | { __typename?: 'IssueState' } | { __typename?: 'IssueTemplate' } | { __typename?: 'IssueType' } | { __typename?: 'Label' } | { __typename?: 'OutgoingRelationTypeChangedEvent' } | { __typename?: 'PriorityChangedEvent' } | { __typename?: 'Project' } | { __typename?: 'ProjectPermission' } | { __typename?: 'RelatedByIssueEvent' } | { __typename?: 'Relation' } | { __typename?: 'RelationCondition' } | { __typename?: 'RelationTemplate' } | { __typename?: 'RemovedAffectedEntityEvent' } | { __typename?: 'RemovedArtefactEvent' } | { __typename?: 'RemovedAssignmentEvent' } | { __typename?: 'RemovedFromPinnedIssuesEvent' } | { __typename?: 'RemovedFromTrackableEvent' } | { __typename?: 'RemovedIncomingRelationEvent' } | { __typename?: 'RemovedLabelEvent' } | { __typename?: 'RemovedOutgoingRelationEvent' } | { __typename?: 'RemovedTemplatedFieldEvent' } | { __typename?: 'SpentTimeChangedEvent' } | { __typename?: 'StartDateChangedEvent' } | { __typename?: 'StateChangedEvent' } | { __typename?: 'StrokeStyle' } | { __typename?: 'TemplateChangedEvent' } | { __typename?: 'TemplatedFieldChangedEvent' } | { __typename?: 'TitleChangedEvent' } | { __typename?: 'TypeChangedEvent' } | null };

export type GetImsUsersByTemplatedFieldValuesQueryVariables = Exact<{
  imsFilterInput: ImsFilterInput;
  userFilterInput: ImsUserFilterInput;
}>;


export type GetImsUsersByTemplatedFieldValuesQuery = { __typename?: 'Query', imss: { __typename: 'IMSConnection', nodes: Array<{ __typename: 'IMS', id: string, users: { __typename: 'IMSUserConnection', nodes: Array<{ __typename: 'IMSUser', id: string }> } }> } };

export type CreateNewImsUserInImsMutationVariables = Exact<{
  input: CreateImsUserInput;
}>;


export type CreateNewImsUserInImsMutation = { __typename?: 'Mutation', createIMSUser: { __typename: 'CreateIMSUserPayload', imsUser: { __typename: 'IMSUser', id: string } } };

export type GetBasicGropiusUserDataQueryVariables = Exact<{
  id: Scalars['ID'];
}>;


export type GetBasicGropiusUserDataQuery = { __typename?: 'Query', node?: { __typename?: 'AddedAffectedEntityEvent' } | { __typename?: 'AddedArtefactEvent' } | { __typename?: 'AddedLabelEvent' } | { __typename?: 'AddedToPinnedIssuesEvent' } | { __typename?: 'AddedToTrackableEvent' } | { __typename?: 'AggregatedIssue' } | { __typename?: 'AggregatedIssueRelation' } | { __typename?: 'Artefact' } | { __typename?: 'ArtefactTemplate' } | { __typename?: 'Assignment' } | { __typename?: 'AssignmentType' } | { __typename?: 'AssignmentTypeChangedEvent' } | { __typename?: 'Body' } | { __typename?: 'Component' } | { __typename?: 'ComponentPermission' } | { __typename?: 'ComponentTemplate' } | { __typename?: 'ComponentVersion' } | { __typename?: 'ComponentVersionTemplate' } | { __typename?: 'DueDateChangedEvent' } | { __typename?: 'EstimatedTimeChangedEvent' } | { __typename?: 'FillStyle' } | { __typename?: 'GlobalPermission' } | { __typename: 'GropiusUser', id: string, username: string, displayName: string, email?: string | null } | { __typename?: 'IMS' } | { __typename?: 'IMSIssue' } | { __typename?: 'IMSIssueTemplate' } | { __typename?: 'IMSPermission' } | { __typename?: 'IMSProject' } | { __typename?: 'IMSProjectTemplate' } | { __typename?: 'IMSTemplate' } | { __typename?: 'IMSUser' } | { __typename?: 'IMSUserTemplate' } | { __typename?: 'IncomingRelationTypeChangedEvent' } | { __typename?: 'Interface' } | { __typename?: 'InterfaceDefinition' } | { __typename?: 'InterfaceDefinitionTemplate' } | { __typename?: 'InterfacePart' } | { __typename?: 'InterfacePartTemplate' } | { __typename?: 'InterfaceSpecification' } | { __typename?: 'InterfaceSpecificationDerivationCondition' } | { __typename?: 'InterfaceSpecificationTemplate' } | { __typename?: 'InterfaceSpecificationVersion' } | { __typename?: 'InterfaceSpecificationVersionTemplate' } | { __typename?: 'InterfaceTemplate' } | { __typename?: 'IntraComponentDependencyParticipant' } | { __typename?: 'IntraComponentDependencySpecification' } | { __typename?: 'Issue' } | { __typename?: 'IssueComment' } | { __typename?: 'IssuePriority' } | { __typename?: 'IssueRelation' } | { __typename?: 'IssueRelationType' } | { __typename?: 'IssueState' } | { __typename?: 'IssueTemplate' } | { __typename?: 'IssueType' } | { __typename?: 'Label' } | { __typename?: 'OutgoingRelationTypeChangedEvent' } | { __typename?: 'PriorityChangedEvent' } | { __typename?: 'Project' } | { __typename?: 'ProjectPermission' } | { __typename?: 'RelatedByIssueEvent' } | { __typename?: 'Relation' } | { __typename?: 'RelationCondition' } | { __typename?: 'RelationTemplate' } | { __typename?: 'RemovedAffectedEntityEvent' } | { __typename?: 'RemovedArtefactEvent' } | { __typename?: 'RemovedAssignmentEvent' } | { __typename?: 'RemovedFromPinnedIssuesEvent' } | { __typename?: 'RemovedFromTrackableEvent' } | { __typename?: 'RemovedIncomingRelationEvent' } | { __typename?: 'RemovedLabelEvent' } | { __typename?: 'RemovedOutgoingRelationEvent' } | { __typename?: 'RemovedTemplatedFieldEvent' } | { __typename?: 'SpentTimeChangedEvent' } | { __typename?: 'StartDateChangedEvent' } | { __typename?: 'StateChangedEvent' } | { __typename?: 'StrokeStyle' } | { __typename?: 'TemplateChangedEvent' } | { __typename?: 'TemplatedFieldChangedEvent' } | { __typename?: 'TitleChangedEvent' } | { __typename?: 'TypeChangedEvent' } | null };

export type GetUserByNameQueryVariables = Exact<{
  username: Scalars['String'];
}>;


export type GetUserByNameQuery = { __typename?: 'Query', gropiusUser: { __typename: 'GropiusUser', id: string, username: string, displayName: string, email?: string | null } };

export type CheckUserIsAdminQueryVariables = Exact<{
  id: Scalars['ID'];
}>;


export type CheckUserIsAdminQuery = { __typename?: 'Query', node?: { __typename: 'AddedAffectedEntityEvent' } | { __typename: 'AddedArtefactEvent' } | { __typename: 'AddedLabelEvent' } | { __typename: 'AddedToPinnedIssuesEvent' } | { __typename: 'AddedToTrackableEvent' } | { __typename: 'AggregatedIssue' } | { __typename: 'AggregatedIssueRelation' } | { __typename: 'Artefact' } | { __typename: 'ArtefactTemplate' } | { __typename: 'Assignment' } | { __typename: 'AssignmentType' } | { __typename: 'AssignmentTypeChangedEvent' } | { __typename: 'Body' } | { __typename: 'Component' } | { __typename: 'ComponentPermission' } | { __typename: 'ComponentTemplate' } | { __typename: 'ComponentVersion' } | { __typename: 'ComponentVersionTemplate' } | { __typename: 'DueDateChangedEvent' } | { __typename: 'EstimatedTimeChangedEvent' } | { __typename: 'FillStyle' } | { __typename: 'GlobalPermission' } | { __typename: 'GropiusUser', id: string, isAdmin: boolean } | { __typename: 'IMS' } | { __typename: 'IMSIssue' } | { __typename: 'IMSIssueTemplate' } | { __typename: 'IMSPermission' } | { __typename: 'IMSProject' } | { __typename: 'IMSProjectTemplate' } | { __typename: 'IMSTemplate' } | { __typename: 'IMSUser' } | { __typename: 'IMSUserTemplate' } | { __typename: 'IncomingRelationTypeChangedEvent' } | { __typename: 'Interface' } | { __typename: 'InterfaceDefinition' } | { __typename: 'InterfaceDefinitionTemplate' } | { __typename: 'InterfacePart' } | { __typename: 'InterfacePartTemplate' } | { __typename: 'InterfaceSpecification' } | { __typename: 'InterfaceSpecificationDerivationCondition' } | { __typename: 'InterfaceSpecificationTemplate' } | { __typename: 'InterfaceSpecificationVersion' } | { __typename: 'InterfaceSpecificationVersionTemplate' } | { __typename: 'InterfaceTemplate' } | { __typename: 'IntraComponentDependencyParticipant' } | { __typename: 'IntraComponentDependencySpecification' } | { __typename: 'Issue' } | { __typename: 'IssueComment' } | { __typename: 'IssuePriority' } | { __typename: 'IssueRelation' } | { __typename: 'IssueRelationType' } | { __typename: 'IssueState' } | { __typename: 'IssueTemplate' } | { __typename: 'IssueType' } | { __typename: 'Label' } | { __typename: 'OutgoingRelationTypeChangedEvent' } | { __typename: 'PriorityChangedEvent' } | { __typename: 'Project' } | { __typename: 'ProjectPermission' } | { __typename: 'RelatedByIssueEvent' } | { __typename: 'Relation' } | { __typename: 'RelationCondition' } | { __typename: 'RelationTemplate' } | { __typename: 'RemovedAffectedEntityEvent' } | { __typename: 'RemovedArtefactEvent' } | { __typename: 'RemovedAssignmentEvent' } | { __typename: 'RemovedFromPinnedIssuesEvent' } | { __typename: 'RemovedFromTrackableEvent' } | { __typename: 'RemovedIncomingRelationEvent' } | { __typename: 'RemovedLabelEvent' } | { __typename: 'RemovedOutgoingRelationEvent' } | { __typename: 'RemovedTemplatedFieldEvent' } | { __typename: 'SpentTimeChangedEvent' } | { __typename: 'StartDateChangedEvent' } | { __typename: 'StateChangedEvent' } | { __typename: 'StrokeStyle' } | { __typename: 'TemplateChangedEvent' } | { __typename: 'TemplatedFieldChangedEvent' } | { __typename: 'TitleChangedEvent' } | { __typename: 'TypeChangedEvent' } | null };

export type GetAllGrpiusUsersQueryVariables = Exact<{ [key: string]: never; }>;


export type GetAllGrpiusUsersQuery = { __typename?: 'Query', gropiusUserIds: Array<string> };

export type CreateNewUserMutationVariables = Exact<{
  input: CreateGropiusUserInput;
}>;


export type CreateNewUserMutation = { __typename?: 'Mutation', createGropiusUser: { __typename?: 'CreateGropiusUserPayload', gropiusUser: { __typename: 'GropiusUser', id: string, username: string, displayName: string, email?: string | null } } };

export type SetImsUserLinkMutationVariables = Exact<{
  gropiusUserId: Scalars['ID'];
  imsUserId: Scalars['ID'];
}>;


export type SetImsUserLinkMutation = { __typename?: 'Mutation', updateIMSUser: { __typename: 'UpdateIMSUserPayload', imsUser: { __typename: 'IMSUser', id: string } } };

export type UserDataFragment = { __typename: 'GropiusUser', id: string, username: string, displayName: string, email?: string | null };

export const ImsUserWithDetailFragmentDoc = gql`
    fragment ImsUserWithDetail on IMSUser {
  __typename
  id
  username
  displayName
  email
  templatedFields {
    __typename
    name
    value
  }
  ims {
    __typename
    id
    name
    description
    templatedFields {
      __typename
      name
      value
    }
  }
}
    `;
export const UserDataFragmentDoc = gql`
    fragment UserData on GropiusUser {
  __typename
  id
  username
  displayName
  email
}
    `;
export const GetBasicImsUserDataDocument = gql`
    query getBasicImsUserData($imsUserId: ID!) {
  node(id: $imsUserId) {
    __typename
    id
  }
}
    `;
export const GetImsUserDetailsDocument = gql`
    query getImsUserDetails($imsUserId: ID!) {
  node(id: $imsUserId) {
    ...ImsUserWithDetail
  }
}
    ${ImsUserWithDetailFragmentDoc}`;
export const GetImsUsersByTemplatedFieldValuesDocument = gql`
    query getImsUsersByTemplatedFieldValues($imsFilterInput: IMSFilterInput!, $userFilterInput: IMSUserFilterInput!) {
  imss(filter: $imsFilterInput) {
    __typename
    nodes {
      __typename
      id
      users(filter: $userFilterInput) {
        __typename
        nodes {
          __typename
          id
        }
      }
    }
  }
}
    `;
export const CreateNewImsUserInImsDocument = gql`
    mutation createNewImsUserInIms($input: CreateIMSUserInput!) {
  createIMSUser(input: $input) {
    __typename
    imsUser {
      __typename
      id
    }
  }
}
    `;
export const GetBasicGropiusUserDataDocument = gql`
    query getBasicGropiusUserData($id: ID!) {
  node(id: $id) {
    ...UserData
  }
}
    ${UserDataFragmentDoc}`;
export const GetUserByNameDocument = gql`
    query getUserByName($username: String!) {
  gropiusUser(username: $username) {
    ...UserData
  }
}
    ${UserDataFragmentDoc}`;
export const CheckUserIsAdminDocument = gql`
    query checkUserIsAdmin($id: ID!) {
  node(id: $id) {
    __typename
    ... on GropiusUser {
      __typename
      id
      isAdmin
    }
  }
}
    `;
export const GetAllGrpiusUsersDocument = gql`
    query getAllGrpiusUsers {
  gropiusUserIds
}
    `;
export const CreateNewUserDocument = gql`
    mutation createNewUser($input: CreateGropiusUserInput!) {
  createGropiusUser(input: $input) {
    gropiusUser {
      ...UserData
    }
  }
}
    ${UserDataFragmentDoc}`;
export const SetImsUserLinkDocument = gql`
    mutation setImsUserLink($gropiusUserId: ID!, $imsUserId: ID!) {
  updateIMSUser(input: {id: $imsUserId, gropiusUser: $gropiusUserId}) {
    __typename
    imsUser {
      __typename
      id
    }
  }
}
    `;

export type SdkFunctionWrapper = <T>(action: (requestHeaders?:Record<string, string>) => Promise<T>, operationName: string, operationType?: string) => Promise<T>;


const defaultWrapper: SdkFunctionWrapper = (action, _operationName, _operationType) => action();

export function getSdk(client: GraphQLClient, withWrapper: SdkFunctionWrapper = defaultWrapper) {
  return {
    getBasicImsUserData(variables: GetBasicImsUserDataQueryVariables, requestHeaders?: Dom.RequestInit["headers"]): Promise<GetBasicImsUserDataQuery> {
      return withWrapper((wrappedRequestHeaders) => client.request<GetBasicImsUserDataQuery>(GetBasicImsUserDataDocument, variables, {...requestHeaders, ...wrappedRequestHeaders}), 'getBasicImsUserData', 'query');
    },
    getImsUserDetails(variables: GetImsUserDetailsQueryVariables, requestHeaders?: Dom.RequestInit["headers"]): Promise<GetImsUserDetailsQuery> {
      return withWrapper((wrappedRequestHeaders) => client.request<GetImsUserDetailsQuery>(GetImsUserDetailsDocument, variables, {...requestHeaders, ...wrappedRequestHeaders}), 'getImsUserDetails', 'query');
    },
    getImsUsersByTemplatedFieldValues(variables: GetImsUsersByTemplatedFieldValuesQueryVariables, requestHeaders?: Dom.RequestInit["headers"]): Promise<GetImsUsersByTemplatedFieldValuesQuery> {
      return withWrapper((wrappedRequestHeaders) => client.request<GetImsUsersByTemplatedFieldValuesQuery>(GetImsUsersByTemplatedFieldValuesDocument, variables, {...requestHeaders, ...wrappedRequestHeaders}), 'getImsUsersByTemplatedFieldValues', 'query');
    },
    createNewImsUserInIms(variables: CreateNewImsUserInImsMutationVariables, requestHeaders?: Dom.RequestInit["headers"]): Promise<CreateNewImsUserInImsMutation> {
      return withWrapper((wrappedRequestHeaders) => client.request<CreateNewImsUserInImsMutation>(CreateNewImsUserInImsDocument, variables, {...requestHeaders, ...wrappedRequestHeaders}), 'createNewImsUserInIms', 'mutation');
    },
    getBasicGropiusUserData(variables: GetBasicGropiusUserDataQueryVariables, requestHeaders?: Dom.RequestInit["headers"]): Promise<GetBasicGropiusUserDataQuery> {
      return withWrapper((wrappedRequestHeaders) => client.request<GetBasicGropiusUserDataQuery>(GetBasicGropiusUserDataDocument, variables, {...requestHeaders, ...wrappedRequestHeaders}), 'getBasicGropiusUserData', 'query');
    },
    getUserByName(variables: GetUserByNameQueryVariables, requestHeaders?: Dom.RequestInit["headers"]): Promise<GetUserByNameQuery> {
      return withWrapper((wrappedRequestHeaders) => client.request<GetUserByNameQuery>(GetUserByNameDocument, variables, {...requestHeaders, ...wrappedRequestHeaders}), 'getUserByName', 'query');
    },
    checkUserIsAdmin(variables: CheckUserIsAdminQueryVariables, requestHeaders?: Dom.RequestInit["headers"]): Promise<CheckUserIsAdminQuery> {
      return withWrapper((wrappedRequestHeaders) => client.request<CheckUserIsAdminQuery>(CheckUserIsAdminDocument, variables, {...requestHeaders, ...wrappedRequestHeaders}), 'checkUserIsAdmin', 'query');
    },
    getAllGrpiusUsers(variables?: GetAllGrpiusUsersQueryVariables, requestHeaders?: Dom.RequestInit["headers"]): Promise<GetAllGrpiusUsersQuery> {
      return withWrapper((wrappedRequestHeaders) => client.request<GetAllGrpiusUsersQuery>(GetAllGrpiusUsersDocument, variables, {...requestHeaders, ...wrappedRequestHeaders}), 'getAllGrpiusUsers', 'query');
    },
    createNewUser(variables: CreateNewUserMutationVariables, requestHeaders?: Dom.RequestInit["headers"]): Promise<CreateNewUserMutation> {
      return withWrapper((wrappedRequestHeaders) => client.request<CreateNewUserMutation>(CreateNewUserDocument, variables, {...requestHeaders, ...wrappedRequestHeaders}), 'createNewUser', 'mutation');
    },
    setImsUserLink(variables: SetImsUserLinkMutationVariables, requestHeaders?: Dom.RequestInit["headers"]): Promise<SetImsUserLinkMutation> {
      return withWrapper((wrappedRequestHeaders) => client.request<SetImsUserLinkMutation>(SetImsUserLinkDocument, variables, {...requestHeaders, ...wrappedRequestHeaders}), 'setImsUserLink', 'mutation');
    }
  };
}
export type Sdk = ReturnType<typeof getSdk>;