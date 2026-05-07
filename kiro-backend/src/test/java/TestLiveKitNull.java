import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;

public class TestLiveKitNull {
    public static void main(String[] args) {
        try {
            AccessToken token = new AccessToken("devkey", "secret");
            token.setName(null);
            token.setIdentity("test");
            token.addGrants(new RoomJoin(true), new RoomName("room"));
            System.out.println(token.toJwt());
            System.out.println("SUCCESS NULL NAME");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
