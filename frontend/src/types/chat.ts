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

