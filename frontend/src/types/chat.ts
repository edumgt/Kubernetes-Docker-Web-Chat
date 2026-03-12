export type SearchUser = {
  email: string;
};

export type ConversationSummary = {
  conversationId: string;
  peerUser: string;
  unreadCount: number;
  lastMessageAt?: string;
  lastReadAt?: string;
  lastMessagePreview?: string;
};

export type PrivateConversation = {
  conversationId: string;
  sender: string;
  recipient: string;
};

export type ChatMessage = {
  id?: string;
  sender: string;
  content: string;
  timestamp?: string;
  conversationId?: string;
};

export type ReadReceiptEvent = {
  conversationId: string;
  reader: string;
  readAt: string;
};

export type Profile = {
  email: string;
  displayName?: string;
  profilePicUrl?: string;
};

export type AdminNodeStatus = {
  name: string;
  ready: boolean;
  status: string;
  role: string;
  kubeletVersion: string;
  internalIp: string;
};

export type AdminPodStatus = {
  name: string;
  namespace: string;
  phase: string;
  ready: string;
  restartCount: number;
  nodeName: string;
  podIp: string;
  app: string;
  createdAt: string;
};

export type AdminChatMetrics = {
  totalRoomCount: number;
  privateRoomCount: number;
  participantCount: number;
};

export type AdminOverview = {
  generatedAt: string;
  namespace: string;
  apiServer: string;
  chatMetrics: AdminChatMetrics;
  nodes: AdminNodeStatus[];
  pods: AdminPodStatus[];
  podPhaseSummary: Record<string, number>;
  kubernetesError?: string | null;
};

export type AdminSession = {
  authenticated: boolean;
  email: string;
};
