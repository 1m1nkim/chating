--------------------------------------------------------------------------------

WebSocket이란 ?
- 웹에서 하나의 TCP 연결을 통해 양방향 통신을 제공하는 프로토콜
(tcp 버전의 socket 이라 생각하면 편함)

```
Websocket의 동작과정
- TCP/IP 접속 요청 (클라이언트)
- TCP/IP 접속 수락 (서버)
- 웹소켓 열기 핸드쉐이크 요청 (클라이언트)
- 웹소켓 열기 핸드쉐이크 수락 (서버)
- 웹소켓 데이터 송, 수신 (클라이언트, 서버)
```

- websocket은 http 기반 양방향 통신이고(pull-duplex) 일반적인 socket과 달리 HTTP 80 포트를 사용하여 방화벽에 제약이 없다

- 접속까지는 HTTP 사용 -> 그 이후 WebSocket 프로토콜 통신

--------------------------------------------------------------------------------

STOMP(Simple Text Oriented Messaging Protocol)

- Websocket은 발신자와 수신자를 서버에서 직접 관리하고 WebSocketHandler를 만들어 클라이언트로부터 들어오는 메시지를 `직접` 다른 사용자에게 전달해줬어야하는 단점이 있었다.

- STOMP는 이를 보완하여 pub/sub 기반으로 동작하여 메시지 송수신에 대한 처리를 어노테이션인 @MessageMapping등으로 효율적으로 해결했다.

- pub/sub은 송, 수신자의 구분이 명확하여 개발자 입장에서 편하게 개발할 수 있다 .

1. 채팅방은 topic을 생성한 후
2. 생성한 topic을 구독 - 채팅방을 WebSocket이 연결되어 있는 동안 연결한다는 느낌
3. 새로운 채팅이 송신(pub)되면 연결된 topic으로 수신(sub)을 하는 개념

- 메세지의 헤더에 값을 줄 수 있어 헤더 기반 통신 시 인증 처리 가능

- 서버는 메세지를 외부 broker에게 전달 -> broker는 Websocket으로 연결된 클라이언트에게 메세지를 전달하는 구조
 - 이와 같은 구조 덕분에 HTTP 기반의 보안 설정과 공통된 검증을 적용할 수 있음

--------------------------------------------------------------------------------

SockJs란 무엇인가 ?
- WebSocket은 FireFox, Chrome, Edge, Whale 브라우저에서는 정상 작동 하지만, 모바일 Chrome이나 Internet Explorer에서는 동작하지 않는 이슈가 있다.
- Server-Client 중간에 위치한 Proxy가 특정 Header를 인식 못한다거나, 임의로 Connection을 종료시킬 수도 있음.

- WebSocket 을 욱선적으로 통신하고 실패할 경우 Emulation 인 SockJs를 사용하여 재연결을 시도하는 방식

- 이 Emulation를 제공하는 javascript 라이브러리가 SockJs

- 스프링에서는 보통 SockJs 라이브러리를 사용하는 것이 일반적

SockJS는 어플리케이션이 WebSocket API를 사용하도록 허용하지만, 브라우저에서 WebSocket을 지원하지 않는 경우에 대안으로 어플리케이션의 코드를 변경할 필요 없이 런타임에 필요할 때 대체해준다.

--------------------------------------------------------------------------------

백엔드

WebsocketConfig 
1. stomp의 엔드포인트를 ws-chat으로 설정
2. 메세지 브로커를 /topic 으로 설정 후 클라이언트가 메시지를 보낼 때 /app 프리픽스를 붙힌다

- 프리픽스를 붙히는 이유는 @MessageMapping 핸들러로 라우팅 되도록 하기 위한 기준
- 클라이언트가 보낸 메세지는 /app 프리픽스를 붙여 전송되고 서버는 이 경로를 통해 해당 메시지를 처리
- 서버가 클라이언트로 메세지를 전송할 때는 /topic 밑에 전송되어 클라이언트는 이 경로를 구독하여 실시간 업데이트를 받음


클라이언트가 서버로 메세지를 보내기 위해 stomp 객체를 사용


ChatController
1. @MessageMapping("/chat.send")로 클라이언트가 전송한 메시지를 수신
2. 수신한 메시지에 대한 실행
  - setRoomId(sender와 receiver를 기준으로 채팅방 ID를 생성)
  - setTimeStamp(방의 만들어진 시간, 추후 사용자의 퇴장값으로 사용)
  - saveMessage(메시지 db에 저장하는 로직, redis 변환)
  - subscribeChatRoom(채팅방의 존재 여부 확인 후 구독, 없으면 생성)
  - messagingTemplate.convertAndSend(구독 된 채팅방에 실시간으로 메시지 전송)
  - unreadCount(메시지를 보낸 시간과 퇴장 값을 비교하여 읽지 않은 메시지 확인)

ChatRestController
1. @GetMapping("/history")
(sender와 receiver를 기준으로 채팅방 검색 후 메시지 조회)
2. @GetMapping("/historyByRoom")
(roomid를 기준으로 채팅방 검색 후 메시지 조회)

ChatRoomRestController
1. @PostMapping("/{roomId}/read")
(채팅방을 읽었으면 markChatRoomAsRead 실행, 현재 채팅방과 로그인 한 아이디를 확인해서 채팅방의 timestamp 갱신)
2. @GetMapping
(구독한 채팅방들을 찾은 후, 현재 상태를 계산 - 읽지않은 수, 누구와의 채팅방인지, 의 정보를 담아 채팅방 리스트를 보여줌)
3.@PostMapping("/leave")
(채팅방을 나갈 시 markChatRoomAsRead 실행, 현재 채팅방과 로그인 한 아이디를 확인해서 채팅방의 timestamp 갱신)

ChatRoomResponse
1. 채팅방의 이름을 반환하는 responseDto

UnreadCountUpdate
1. 읽지 않은 메세지를 업데이트하는 클래스

ChatMessage
1. 메세지의 송, 수신자, 채팅방의 id, 메세지 내용,  등을 저장하는 entity

ChatRoom
1.  송, 수신자, 채팅방의 id, 사용자별 마지막 읽은 시간 등을 저장하는 entity

ChatMessageRepository
1. countUnreadMessages를 JPQL 처리 해서 채팅방, 보낸사람, 보낸 시각을 계산 해서 읽지 않은 메시지 수 계산

ChatService
1. getRoomId 
(송, 수신자의 id를 기반으로  채팅방 ID 생성, 게시글 id도 추가하면 좋을듯)
2. getMessages,getMessagesByRoomId
(sender, receiver 또는 roomid를 기반으로 메시지 조회)
3. subscribeChatRoom, 
(채팅방이 없을 경우 새로 생성하고, 존재하면 기존 채팅방을 반환합니다.)
4. getSubscribedChatRooms
(사용자의 채팅방 list 반환)
5. getUnreadCount
(읽지않은 메세지를 시간의 맞춰 개수를 가져옵니다.)
6.markChatRoomAsRead
(채팅방의 퇴장 시간을 기록함)



--------------------------------------------------------------------------------

npm install sockjs-client
npm install @stomp/stompjs 

프론트

/api/auth/me로 현재 사용자 가져옴

로그인 상태 확인 한 후 /api/chatrooms?username=
으로 구독중인 채팅방 목록과 메세지 개수를 받아옴

채팅방클릭 또는 새채팅을 클릭하면 채팅방 입장 후 
GET /api/chat/historyByRoom?roomId= 로 채팅방의 채팅내역을 불러옴
POST /api/chatrooms/{roomId}/read?username= 로 미확인 메시지 초기화

채팅방 입장 
- 채팅방 입장 시 WebSocket 연결(ws-chat 엔드포인트)을 설정
const socket = new SockJS("http://localhost:8080/ws-chat");

- stomp 클라이언트를 통해 /topic/chat/{roomid} 구독 시작 후 메시지를 수신하면 채팅 내역에 추가 
(만들기만 하고 메시지 보내지 않으면 채팅방을 생성하지 않음)

- 메세지 전송 시 /app/chat.send로 전송, 백엔드에서 메세지 db에 저장 후 다시 실시간 전송

- 미확인 메세지 개수 업데이트를 위한 /topic/unreadCount/{receiver} 업데이트

"클라이언트가 상대방에게 메시지를 전송하면, ChatController의 sendMessage 메서드에서 처리됩니다. 여기서 먼저 메시지를 데이터베이스에 저장한 후, 채팅방(topic)에 메시지를 전송합니다. 이후, ChatService.getUnreadCount를 호출하여 수신자가 아직 읽지 않은 메시지의 개수를 계산합니다. 이 업데이트된 개수는 UnreadCountUpdate DTO에 담아서, messagingTemplate.convertAndSend를 통해 '/topic/unreadCount/{receiver}' 경로로 전송됩니다. 이로 인해 상대방의 클라이언트는 구독 중인 '/topic/unreadCount/{receiver}' 경로를 통해 실시간으로 미확인 메시지 개수가 업데이트 되는 것을 확인할 수 있습니다."


```
const sendMessage = () => {
        if (!stompClient) {
            alert("메시지 전송이 불가능합니다.");
            return;
        }
        if (!content.trim()) {
            alert("메시지를 입력해주세요.");
            return;
        }
        // 메시지 전송 시점에 정확한 시간을 기록
        const chatMessage: ChatMessage = {
            sender: username,
            receiver: receiver,
            content: content,
            timestamp: new Date().toISOString(),
        };
        stompClient.send("/app/chat.send", {}, JSON.stringify(chatMessage));
        setContent("");
    };
```
프론트에서 /app/chat.send로 넘기는 이유, 
registry.setApplicationDestinationPrefixes("/app");로 프리픽스 설정 해둬서 @MessageMapping("/chat.send")가 붙은 메서드로 메시지를 라우팅


결론 클라이언트는 메시지 전송 시 /app/chat.send로 보내고, 서버는 처리를 한 후 해당 채팅방의 토픽인 /topic/chat/{roomId}로 메시지를 다시 보내는 구조입니다.
