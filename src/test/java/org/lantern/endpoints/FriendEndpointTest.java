package org.lantern.endpoints;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Test;
import org.lantern.TestingUtils;
import org.lantern.oauth.OauthUtils;
import org.lantern.oauth.RefreshToken;
import org.lantern.state.ClientFriend;
import org.lantern.state.Friend;
import org.lantern.state.Mode;
import org.lantern.state.Model;
import org.lantern.util.HttpClientFactory;
import org.lantern.util.Stopwatch;
import org.lantern.util.StopwatchManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for the friends syncing REST API.
 */
public class FriendEndpointTest {
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Test
    public void testFriendEndpiont() throws Exception {
        TestingUtils.doWithWithGetModeProxy(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                final HttpClientFactory httpClientFactory = 
                        TestingUtils.newHttClientFactory();
                final Model model = TestingUtils.newModel();
                model.getSettings().setMode(Mode.give);
                final OauthUtils utils = new OauthUtils(httpClientFactory, model, new RefreshToken(model));
                final FriendApi api = new FriendApi(utils);
                
                // First just clear the existing friends.
                List<ClientFriend> friends = api.listFriends();
                    
                log.debug("Deleting all friends from: {}", friends);
                for (final ClientFriend f : friends) {
                    final Long id = f.getId();
                    api.removeFriend(id);
                    // Give the db a chance to sync.
                    log.debug("Removing friend: {}", f);
                    //Thread.sleep(50);
                }
                
                final ClientFriend friend = new ClientFriend();
                friend.setEmail("test@test.com");
                friend.setName("Tester");
                api.insertFriend(friend);
                
                final Stopwatch friendsWatch = 
                    StopwatchManager.getStopwatch("friends-api", 
                        "org.lantern", "listFriends");
                
                friends = null;
                for (int i = 0; i < 2; i++) {
                    friendsWatch.start();
                    friends = api.listFriends();
                    
                    friendsWatch.stop();
                    log.debug("Deleting all friends from: {}", friends);
                    for (final ClientFriend f : friends) {
                        final Long id = f.getId();
                        api.removeFriend(id);
                        // Give the db a chance to sync.
                        log.debug("Removing friend: {}", f);
                        //Thread.sleep(200);
                    }
                    friendsWatch.logSummary();
                }
                friendsWatch.logSummary();
                StopwatchManager.logSummaries("org.lantern");
                
                /*
                log.debug("Deleting all friends from: {}", friends);
                for (final ClientFriend f : friends) {
                    final Long id = f.getId();
                    api.removeFriend(id);
                    // Give the db a chance to sync.
                    log.debug("Removing friend: {}", f);
                    Thread.sleep(2000);
                }
                */
                
                //Thread.sleep(2000);
                
                final List<ClientFriend> postDelete = api.listFriends();
                
                assertEquals("Did not successfully delete friends?", 0, postDelete.size());
                
                final ClientFriend inserted = api.insertFriend(friend);
                
                final String updatedName = "brandnew@email.com";
                inserted.setEmail(updatedName);
                final Friend updated = api.updateFriend(inserted);
                
                //Thread.sleep(4000);
                assertEquals(updatedName, updated.getEmail());
                
                final List<ClientFriend> newList = api.listFriends();
                for (final ClientFriend f : newList) {
                    assertEquals(updatedName, f.getEmail());
                    
                    final Long id = f.getId();
                    final Friend get = api.getFriend(id);
                    assertEquals(id, get.getId());
                    
                    api.removeFriend(id);
                    // Give the db a chance to sync.
                    //Thread.sleep(100);
                }
                
                final List<ClientFriend> empty = api.listFriends();
                assertEquals(0, empty.size());
                return null;
            }
        });
    }
}