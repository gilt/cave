## Data Grouping

The top level group is called **organization**. This is because we built CAVE like a SaaS, which can scale to accommodate multiple company-sized customers, with all their metrics. For a local deployment of CAVE, this top level group can be assigned to departments, or other organizational structures.

The user who creates an organization automatically becomes the Administrator. This role allows the user to create teams inside the organization, invite other people to the organization, and assign roles for them.

Under each organization, data can be further grouped into **teams**. A team can only be created by an Administrator of the organization, and no user is assigned permissions by default. The Administrator should add a team Administrator as needed. The team administrator can then add more members to the team.

### User Roles
There are three roles that can apply to a member for each group: Admin, Member, and Viewer.

The **Viewer** role means that the user can only query data belonging to the team or organization. No other operations are possible.

The **Member** role has all the Viewer permissions, as well as permission to get group information, group members list, and manage group's alerts (create, retrieve, modify, delete).

The **Admin** role has all Member privileges, as well as the ability to delete metrics, manage group membership (add users, change roles, remove users), manage group tokens (create token, list tokens, delete token).
