# ☁️ GCP & AWS — Complete Interview Prep Guide

> **Core Services · Containers & Kubernetes · Networking & Security · Real-World Analogies · Interview Q&A**

---

## Table of Contents

1. [Cloud Fundamentals](#cloud-fundamentals)
2. [Core Services Overview](#core-services-overview)
3. [Container & Kubernetes Services](#container--kubernetes-services)
4. [Networking & Security](#networking--security)
5. [GCP vs AWS — Side-by-Side Comparison](#gcp-vs-aws--side-by-side-comparison)
6. [Common Interview Questions](#common-interview-questions)

---

## Cloud Fundamentals

### What is Cloud Computing?
Cloud computing means renting computing resources (servers, storage, databases, networking) over the internet instead of owning physical hardware. You pay for what you use, scale instantly, and let the cloud provider manage the infrastructure.

Think of it like **electricity from the grid** — you don't build your own power plant; you plug in and pay for what you consume.

### The 3 Service Models

| Model | What you manage | What provider manages | Example |
|---|---|---|---|
| **IaaS** (Infrastructure as a Service) | OS, runtime, apps | Hardware, networking | EC2, GCE |
| **PaaS** (Platform as a Service) | Just your app/code | Everything else | App Engine, Elastic Beanstalk |
| **SaaS** (Software as a Service) | Nothing (just use it) | Everything | Gmail, Salesforce |

---

## Core Services Overview

### 🟠 AWS Core Services

#### Compute
**EC2 (Elastic Compute Cloud)**
Virtual machines in the cloud. You choose the OS, CPU, RAM, and storage. Think of it as renting a computer in Amazon's data center.

- **Analogy**: Renting a fully equipped office room — you control everything inside
- **Instance types**: General purpose (t3, m5), Compute optimized (c5), Memory optimized (r5), GPU (p3)
- **Key features**: Auto Scaling, Elastic Load Balancing, Spot Instances (up to 90% cheaper for interruptible workloads)

**Lambda**
Serverless compute — you just write a function and AWS runs it when triggered. No servers to manage.

- **Analogy**: A vending machine — you press a button (trigger), it does one job, then sleeps until next time
- **Triggers**: API Gateway, S3 events, DynamoDB streams, CloudWatch Events, SQS
- **Limits**: 15 min max execution, 10GB memory, 512MB–10GB ephemeral storage

**Elastic Beanstalk**
PaaS for deploying web apps. You upload your code and AWS handles the rest — provisioning, load balancing, scaling, monitoring.

- **Analogy**: A managed restaurant franchise — you provide the recipe (code), they handle the kitchen, staff, and building

---

#### Storage
**S3 (Simple Storage Service)**
Object storage for files of any type — images, videos, backups, logs, static websites. Virtually unlimited capacity.

- **Analogy**: A massive, infinitely large filing cabinet in the cloud
- **Storage classes**: Standard, Intelligent-Tiering, Glacier (archival), Glacier Deep Archive
- **Key features**: Versioning, lifecycle policies, static website hosting, event notifications, presigned URLs

**EBS (Elastic Block Store)**
Persistent disk storage attached to EC2 instances. Like a hard drive for your VM.

- **Analogy**: A USB hard drive plugged into your computer — fast, persistent, but tied to one machine
- **Types**: gp3 (general), io2 (high IOPS), st1 (throughput), sc1 (cold)

**EFS (Elastic File System)**
Shared network file system that multiple EC2 instances can mount simultaneously.

- **Analogy**: A shared network drive in an office — everyone connects to the same storage

---

#### Databases
**RDS (Relational Database Service)**
Managed relational databases. AWS handles backups, patching, replication.

- **Supported engines**: MySQL, PostgreSQL, MariaDB, Oracle, SQL Server, Aurora
- **Aurora**: AWS's own engine — 5x faster than MySQL, 3x faster than PostgreSQL, serverless option available

**DynamoDB**
Fully managed NoSQL database. Single-digit millisecond latency at any scale.

- **Analogy**: A giant, hyper-fast key-value store like a dictionary that never slows down no matter how big it gets
- **Key concepts**: Partition key, Sort key, GSI (Global Secondary Index), LSI (Local Secondary Index)
- **Capacity modes**: On-demand vs. Provisioned

**ElastiCache**
Managed in-memory caching using Redis or Memcached.

- **Analogy**: A whiteboard next to your desk — faster to read from than going to the file cabinet (database) every time

---

#### Messaging & Integration
**SQS (Simple Queue Service)**
Managed message queue. Decouples producers and consumers.

- **Types**: Standard (at-least-once, best-effort ordering) vs. FIFO (exactly-once, strict ordering)

**SNS (Simple Notification Service)**
Pub/Sub messaging. One message, many subscribers (fan-out).

- **Analogy**: A megaphone announcement — one speaker, many listeners (email, SMS, Lambda, SQS)

**EventBridge**
Serverless event bus. Routes events between AWS services and your applications.

---

### 🔵 GCP Core Services

#### Compute
**Compute Engine (GCE)**
Virtual machines on Google's infrastructure. Equivalent to AWS EC2.

- **Key features**: Live migration (VMs migrate during maintenance without downtime), custom machine types (exact CPU/RAM you need), preemptible VMs (like Spot Instances)
- **Analogy**: Same as EC2 — renting a computer, but with Google's network underneath

**Cloud Functions**
Serverless compute, equivalent to AWS Lambda.

- **Triggers**: HTTP, Pub/Sub, Cloud Storage events, Firestore changes
- **Runtimes**: Node.js, Python, Go, Java, Ruby, PHP, .NET

**App Engine**
PaaS for web apps. Standard (sandboxed, fast scaling to zero) and Flexible (Docker-based) environments.

- **Analogy**: Google's managed kitchen — bring your recipe (code), they handle everything else

**Cloud Run**
Run containerized apps in a fully managed serverless environment. Scales to zero when idle.

- **Analogy**: A pop-up restaurant that appears instantly when customers arrive and disappears when they leave — you only pay when serving customers
- **Key differentiator from Lambda**: Works with any language/runtime via containers, not just supported runtimes

---

#### Storage
**Cloud Storage (GCS)**
Object storage equivalent to AWS S3. Globally unified namespace.

- **Storage classes**: Standard, Nearline (monthly access), Coldline (quarterly), Archive
- **Key feature**: Strong consistency — reads always reflect the latest write immediately

**Persistent Disk**
Block storage for Compute Engine VMs, equivalent to AWS EBS.

- **Types**: Standard (HDD), Balanced (SSD), SSD (high performance), Extreme

**Filestore**
Managed NFS file storage, equivalent to AWS EFS.

---

#### Databases
**Cloud SQL**
Managed relational databases — MySQL, PostgreSQL, SQL Server. Equivalent to AWS RDS.

**Cloud Spanner**
Globally distributed, horizontally scalable relational database. Unique to GCP — combines relational guarantees with NoSQL scale.

- **Analogy**: A relational database that can magically scale like NoSQL — the best of both worlds
- **Use case**: Financial systems, global inventory, anything requiring ACID transactions at massive scale

**Firestore**
Managed NoSQL document database. Equivalent to DynamoDB but document-based.

- **Modes**: Native mode (real-time sync, offline support) and Datastore mode (legacy)

**Bigtable**
Managed wide-column NoSQL database for massive analytical workloads (petabytes). Powers Google Search, Maps, Gmail internally.

- **Analogy**: A giant spreadsheet with billions of rows that reads/writes in milliseconds

**BigQuery**
Serverless data warehouse for analytics. Run SQL queries on petabytes of data.

- **Analogy**: A super-powered Excel that can process billions of rows in seconds without any server setup
- **Key features**: Columnar storage, built-in ML (BigQuery ML), separation of storage and compute

---

#### Messaging
**Pub/Sub**
Managed messaging service — equivalent to a combination of AWS SNS + SQS.

- **Analogy**: A newspaper publisher (publisher) sending papers to subscribers — subscribers get every edition regardless of when they subscribed

---

## Container & Kubernetes Services

### 🔵 GKE — Google Kubernetes Engine

Google invented Kubernetes and donated it to open source. GKE is considered the gold standard for managed Kubernetes.

**Key Features:**
- **Autopilot mode**: Fully managed — Google manages nodes, scaling, and infrastructure. You only think about Pods.
- **Standard mode**: You manage node pools but Google manages the control plane
- **Release channels**: Rapid, Regular, Stable — control how fast you get Kubernetes updates
- **Built-in monitoring**: Cloud Monitoring and Cloud Logging integrated out of the box
- **Workload Identity**: Securely connect GKE workloads to GCP services without service account keys

**Analogy**: GKE Autopilot is like hiring a full property management company — you own the apartment (app), they handle everything else (plumbing, electricity, repairs).

```
GKE Cluster Architecture:

  Control Plane (managed by Google)
  ├── API Server
  ├── Scheduler
  ├── etcd
  └── Controller Manager

  Node Pool 1 (e.g., n2-standard-4)     Node Pool 2 (e.g., n1-highmem-8)
  ├── Node 1                             ├── Node 3
  │   ├── Pod A (your app)               │   └── Pod C (ML workload)
  │   └── Pod B (sidecar)               └── Node 4
  └── Node 2                                 └── Pod D
```

---

### 🟠 EKS — Elastic Kubernetes Service

AWS's managed Kubernetes offering.

**Key Features:**
- **Managed control plane**: AWS manages etcd and API server; you manage worker nodes
- **Fargate integration**: Run pods without managing EC2 nodes at all (serverless nodes)
- **EKS Anywhere**: Run EKS on your own on-premises hardware
- **AWS integrations**: Deep integration with IAM, ALB, EBS, EFS, CloudWatch

**EKS vs GKE Key Differences:**

| Feature | GKE | EKS |
|---|---|---|
| Kubernetes origin | Google (created it) | AWS-managed |
| Fully managed option | Autopilot mode | Fargate (partial) |
| Control plane cost | Free (Standard) | $0.10/hr per cluster |
| Upgrade experience | Smoother, automated | More manual effort |
| Ecosystem integration | GCP services | AWS services |

---

### Serverless Container Options

**AWS Fargate**
Run containers without managing EC2 instances. Works with both ECS and EKS.

- **Analogy**: A self-driving car — you decide the destination (container spec), it handles the driving (infrastructure)
- **Pricing**: Per vCPU and memory used per second

**Google Cloud Run**
Run any containerized app serverlessly. Scales to zero.

- **Key advantage over Fargate**: Scales to zero (Fargate always has at least one task running for ECS services)

**AWS ECS (Elastic Container Service)**
AWS's own (non-Kubernetes) container orchestration. Simpler than Kubernetes but less portable.

- **Launch types**: EC2 (you manage nodes) or Fargate (serverless)
- **When to use**: Simpler workloads where Kubernetes overhead isn't justified; all-in on AWS

---

## Networking & Security

### 🌐 Networking Fundamentals

#### VPC (Virtual Private Cloud)
A logically isolated network in the cloud where you launch your resources. Think of it as your own private data center inside AWS/GCP.

- **Analogy**: A gated community — you control who enters, which roads lead where, and what rules apply inside

**AWS VPC:**
- **Subnets**: Public (internet-accessible) or Private (internal only)
- **Internet Gateway**: Allows public subnets to access the internet
- **NAT Gateway**: Allows private subnets to initiate outbound internet connections (but blocks inbound)
- **VPC Peering**: Connect two VPCs privately
- **Transit Gateway**: Hub-and-spoke model to connect many VPCs and on-premises networks
- **PrivateLink**: Access AWS services privately without traffic going over the public internet

**GCP VPC:**
- **Global by default**: Unlike AWS where VPCs are regional, GCP VPCs are global — one VPC spans all regions
- **Subnets are regional**: You create regional subnets within the global VPC
- **Shared VPC**: Share a VPC across multiple GCP projects
- **VPC Peering**: Connect VPCs across projects or organizations
- **Cloud Interconnect / VPN**: Connect on-premises to GCP

---

#### Load Balancing

**AWS Load Balancers:**
- **ALB (Application Load Balancer)**: Layer 7, HTTP/HTTPS, path-based and host-based routing, WebSocket support
- **NLB (Network Load Balancer)**: Layer 4, TCP/UDP, ultra-low latency, static IP support
- **CLB (Classic)**: Legacy, avoid for new workloads
- **GLB (Gateway)**: For deploying network virtual appliances (firewalls, IDS)

**GCP Load Balancers:**
- **Global HTTP(S) LB**: Layer 7, anycast IP, routes to nearest healthy backend globally — single IP for worldwide traffic
- **Regional External LB**: Layer 4/7 within a region
- **Internal LB**: For traffic within your VPC
- **Cloud CDN**: Integrates with HTTP(S) LB for content caching at Google's edge

---

#### DNS
- **AWS Route 53**: DNS service + domain registration + health checks + traffic routing policies (weighted, latency-based, geolocation, failover)
- **GCP Cloud DNS**: Managed DNS, 100% uptime SLA, DNSSEC support

---

### 🔒 Security

#### Identity & Access Management

**AWS IAM:**
- **Users**: Individual human identities
- **Groups**: Collections of users with shared permissions
- **Roles**: Identities assumed by services, applications, or users temporarily (no long-term credentials)
- **Policies**: JSON documents that define permissions (Allow/Deny on Actions for Resources)
- **Best practices**: Principle of least privilege, use roles over users for services, enable MFA, rotate access keys

```json
// Example IAM Policy — Allow S3 read access to specific bucket
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": ["s3:GetObject", "s3:ListBucket"],
    "Resource": ["arn:aws:s3:::my-bucket", "arn:aws:s3:::my-bucket/*"]
  }]
}
```

**GCP IAM:**
- **Members**: Google accounts, service accounts, Google groups, domains
- **Roles**: Primitive (Owner/Editor/Viewer — avoid), Predefined (fine-grained), Custom
- **Bindings**: Attach a role to a member on a resource
- **Service Accounts**: Identities for GCE VMs, GKE pods, Cloud Functions to access GCP services
- **Workload Identity Federation**: Allow external identities (GitHub Actions, AWS) to access GCP without service account keys

---

#### Secrets Management
- **AWS Secrets Manager**: Store, rotate, and retrieve database credentials, API keys, other secrets. Automatic rotation supported.
- **AWS Parameter Store**: Lightweight secrets/config storage, free tier available
- **GCP Secret Manager**: Centralized secret storage with versioning, IAM access control, and audit logging

---

#### Security Services

**AWS:**
- **GuardDuty**: ML-based threat detection — monitors CloudTrail, VPC Flow Logs, DNS logs for suspicious activity
- **AWS WAF**: Web Application Firewall — protect against SQL injection, XSS, rate limiting
- **AWS Shield**: DDoS protection. Standard (free, automatic) and Advanced (paid, 24/7 DDoS response team)
- **Security Hub**: Centralized security posture management and compliance checks
- **Inspector**: Automated vulnerability scanning for EC2 and container images
- **KMS (Key Management Service)**: Create and manage encryption keys

**GCP:**
- **Security Command Center**: Centralized security and risk management platform
- **Cloud Armor**: WAF and DDoS protection, equivalent to AWS WAF + Shield
- **Cloud KMS**: Managed encryption keys
- **Binary Authorization**: Only deploy trusted container images (cryptographic attestation)
- **VPC Service Controls**: Create security perimeters around GCP services to prevent data exfiltration

---

#### Compliance & Audit
- **AWS CloudTrail**: Records all API calls made in your AWS account — who did what, when, from where
- **GCP Cloud Audit Logs**: Admin Activity, Data Access, System Event, and Policy Denied logs
- **AWS Config**: Tracks configuration changes over time and checks compliance against rules
- **GCP Asset Inventory**: Inventory of all GCP resources and their configurations

---

## GCP vs AWS — Side-by-Side Comparison

### Services Mapping

| Category | AWS | GCP |
|---|---|---|
| Virtual Machines | EC2 | Compute Engine |
| Serverless Functions | Lambda | Cloud Functions |
| Managed Containers (K8s) | EKS | GKE |
| Serverless Containers | Fargate / App Runner | Cloud Run |
| PaaS | Elastic Beanstalk | App Engine |
| Object Storage | S3 | Cloud Storage |
| Block Storage | EBS | Persistent Disk |
| File Storage | EFS | Filestore |
| Managed SQL DB | RDS / Aurora | Cloud SQL / Spanner |
| NoSQL DB | DynamoDB | Firestore / Bigtable |
| In-memory Cache | ElastiCache | Memorystore |
| Data Warehouse | Redshift | BigQuery |
| Message Queue | SQS | Pub/Sub |
| Pub/Sub Messaging | SNS | Pub/Sub |
| CDN | CloudFront | Cloud CDN |
| DNS | Route 53 | Cloud DNS |
| Load Balancer | ALB / NLB | Cloud Load Balancing |
| IAM | IAM | Cloud IAM |
| Secrets | Secrets Manager | Secret Manager |
| Monitoring | CloudWatch | Cloud Monitoring |
| Logging | CloudWatch Logs | Cloud Logging |
| CI/CD | CodePipeline / CodeBuild | Cloud Build / Cloud Deploy |
| Infrastructure as Code | CloudFormation | Deployment Manager / Terraform |
| WAF & DDoS | WAF + Shield | Cloud Armor |
| Threat Detection | GuardDuty | Security Command Center |

---

### Key Philosophical Differences

**AWS:**
- Largest market share (~32%), broadest service catalog (200+ services)
- More services = more complexity, but also more choice
- Regional VPCs — you explicitly design multi-region architectures
- Pay-as-you-go pricing with complex pricing tiers

**GCP:**
- Known for data analytics (BigQuery), ML/AI (Vertex AI), and Kubernetes (invented it)
- Global VPC simplifies multi-region networking
- Often better pricing for sustained workloads (sustained use discounts are automatic)
- Stronger on open-source and open standards

---

## Common Interview Questions

### AWS Questions

**Q1. What is the difference between S3 Standard, S3 Intelligent-Tiering, and S3 Glacier?**

> S3 Standard is for frequently accessed data with the highest availability and lowest latency. Intelligent-Tiering automatically moves data between frequent and infrequent access tiers based on usage patterns, optimizing cost without performance impact. Glacier is for archival — extremely cheap but retrieval takes minutes to hours. Use Standard for production data, Intelligent-Tiering when access patterns are unpredictable, and Glacier for compliance/backup data you rarely need.

---

**Q2. What is the difference between Security Groups and NACLs in AWS?**

> Security Groups are stateful firewalls that operate at the instance level — if you allow inbound traffic, the response is automatically allowed outbound. NACLs (Network Access Control Lists) are stateless and operate at the subnet level — you must explicitly allow both inbound AND outbound for a connection. Security Groups are the primary tool; NACLs are an additional layer of defense for subnet-level rules.

---

**Q3. Explain the difference between AWS IAM Roles and IAM Users.**

> IAM Users have permanent long-term credentials (access keys/passwords) and represent a specific human or application. IAM Roles have temporary credentials generated via STS (Security Token Service) and are assumed by entities — like an EC2 instance assuming a role to access S3 without hard-coding credentials. Best practice is to always use Roles for services and applications, never embed access keys in code.

---

**Q4. What is the difference between RDS Multi-AZ and RDS Read Replicas?**

> Multi-AZ is for **high availability** — a standby replica is maintained in a different Availability Zone. If the primary fails, AWS automatically fails over to the standby (no data loss, minimal downtime). It does NOT serve read traffic normally. Read Replicas are for **performance scaling** — they serve read traffic to offload the primary, but they're asynchronously replicated (slight lag) and are NOT for failover. You can promote a Read Replica to a standalone DB manually.

---

**Q5. What is an S3 presigned URL?**

> A presigned URL grants temporary, time-limited access to a private S3 object without requiring AWS credentials. You generate it server-side using your credentials, and share the URL with a client who can then download (or upload) the object directly from S3. Useful for allowing users to upload files directly to S3 without going through your server. The URL expires after a set time (seconds to 7 days).

---

**Q6. How does AWS Lambda handle scaling?**

> Lambda scales automatically and instantly — each incoming request gets its own execution environment (container). If 1,000 requests arrive simultaneously, Lambda spins up 1,000 parallel executions. The default concurrency limit is 1,000 per region (can be increased). Cold starts occur when a new execution environment is initialized — you can mitigate this with Provisioned Concurrency, which keeps environments warm.

---

**Q7. What is the difference between ECS and EKS?**

> ECS is AWS's proprietary container orchestration — simpler, tightly integrated with AWS, no Kubernetes knowledge needed. EKS runs actual Kubernetes — more complex, steeper learning curve, but portable (your workloads can run on any Kubernetes cluster). Choose ECS for simpler AWS-native workloads; choose EKS when you need Kubernetes features, portability, or your team already knows Kubernetes.

---

### GCP Questions

**Q8. What makes Cloud Spanner unique compared to other databases?**

> Cloud Spanner is the only database that combines the consistency guarantees of a relational database (ACID transactions, SQL, foreign keys) with the horizontal scalability of NoSQL. Traditional relational DBs (MySQL, PostgreSQL) can't scale horizontally. NoSQL DBs (DynamoDB, Cassandra) scale horizontally but sacrifice relational features. Spanner achieves both using Google's TrueTime API (atomic clock + GPS) for global timestamp consistency.

---

**Q9. How is GCP's VPC different from AWS's VPC?**

> The biggest difference is scope. AWS VPCs are regional — you create separate VPCs per region and connect them via VPC Peering or Transit Gateway. GCP VPCs are global — a single VPC spans all regions, and you create regional subnets within it. This makes multi-region architectures simpler in GCP — a VM in us-central1 and a VM in europe-west1 can be in the same VPC and communicate privately without peering.

---

**Q10. What is BigQuery and when would you use it over Cloud SQL?**

> BigQuery is a serverless data warehouse optimized for analytical (OLAP) queries — scanning billions of rows across petabytes of data using distributed SQL. Cloud SQL is a traditional relational database optimized for transactional (OLTP) workloads — frequent reads/writes, row-level operations. Use BigQuery for business intelligence, reporting, and data analysis. Use Cloud SQL for your application's operational database (user records, orders, etc.).

---

**Q11. Explain GKE Autopilot vs Standard mode.**

> Standard mode gives you control over node pools — you choose machine types, manage upgrades, and pay for nodes whether or not they're fully utilized. Autopilot is fully managed — GCP manages nodes, scaling, and infrastructure; you only define Pods. Autopilot enforces security best practices by default, and you pay only for Pod resource requests (not idle node capacity). Use Autopilot for most workloads; use Standard when you need specific node configurations or DaemonSets.

---

**Q12. What is Workload Identity in GKE and why is it preferred over service account keys?**

> Workload Identity allows GKE Pods to authenticate to GCP services using Kubernetes service accounts bound to GCP service accounts — without any key files. The alternative is mounting a service account JSON key into the Pod, which is a security risk (keys can be leaked, they don't expire automatically, and rotating them is manual). Workload Identity is preferred because authentication is automatic, keys are never stored anywhere, and IAM controls access centrally.

---

### Cross-Cloud / Conceptual Questions

**Q13. When would you choose GCP over AWS (or vice versa)?**

> Choose **GCP** when your workload is data-heavy (BigQuery is unmatched for analytics), you need a global backbone network (Google's private fiber network), or you're building ML/AI pipelines (Vertex AI, TPUs). GCP is also often more cost-effective for sustained workloads due to automatic sustained use discounts.

> Choose **AWS** when you need the broadest service catalog, your team already has AWS expertise, you're in a highly regulated industry with existing AWS compliance certifications, or you need the largest ecosystem of third-party integrations.

---

**Q14. What is the shared responsibility model in cloud security?**

> Cloud security is a shared responsibility between the cloud provider and the customer. The provider is responsible for security **of** the cloud — physical infrastructure, hardware, networking, hypervisor. The customer is responsible for security **in** the cloud — operating system patches, application security, IAM configuration, data encryption, network configuration. The exact boundary shifts depending on the service model: the provider takes on more responsibility in PaaS/SaaS than in IaaS.

---

**Q15. How do you design for high availability in the cloud?**

> High availability means designing so that your application continues to work even when individual components fail. Key principles: deploy across multiple Availability Zones (or regions for even higher availability), use managed load balancers to distribute traffic, use auto-scaling groups to replace unhealthy instances, use managed databases with Multi-AZ replication, decouple components with queues (SQS/Pub/Sub) so failures don't cascade, and define health checks everywhere. The goal is eliminating every single point of failure.

---

## Summary Cheat Sheet

### AWS in One Line Each
- **EC2** → Virtual servers on demand
- **Lambda** → Run code without servers, triggered by events
- **S3** → Unlimited file storage in the cloud
- **RDS/Aurora** → Managed SQL databases
- **DynamoDB** → Infinitely scalable NoSQL key-value store
- **EKS** → Managed Kubernetes on AWS
- **Fargate** → Run containers without managing servers
- **VPC** → Your private network in AWS (regional)
- **IAM** → Control who can do what in your AWS account
- **CloudWatch** → Monitoring, logs, and alerts
- **Route 53** → DNS + traffic routing + health checks
- **GuardDuty** → AI-powered threat detection

### GCP in One Line Each
- **Compute Engine** → Virtual servers on Google's infrastructure
- **Cloud Functions** → Serverless event-driven functions
- **Cloud Storage** → Globally consistent object storage
- **Cloud SQL / Spanner** → Managed SQL (Spanner = global + scalable)
- **Firestore / Bigtable** → NoSQL (documents vs. wide-column)
- **BigQuery** → Serverless analytics data warehouse
- **GKE** → Best-in-class managed Kubernetes (Google invented it)
- **Cloud Run** → Serverless containers that scale to zero
- **VPC** → Your private network in GCP (global!)
- **Cloud IAM** → Identity and access management
- **Cloud Armor** → WAF + DDoS protection
- **Pub/Sub** → Managed messaging (SNS + SQS combined)

---