package com.braseiro.dnd5e;
import org.junit.Test;
import static org.junit.Assert.*;
public final class RemoteMasterTransportPolicyTest {
 @Test public void temporaryApiFailureCanRetryButAuthenticationCannot(){
  for(int status:new int[]{408,429,500,502,503,504})assertTrue(RemoteMasterTransportPolicy.retryableHttp(status));
  for(int status:new int[]{400,401,403,404})assertFalse(RemoteMasterTransportPolicy.retryableHttp(status));
 }
 @Test public void directRecoveryWorksBeforeAnyFragmentAndAfterInterruptedStream(){
  assertTrue(RemoteMasterTransportPolicy.tryDirectRecovery(true,true));
  assertFalse(RemoteMasterTransportPolicy.tryDirectRecovery(true,false));
  assertFalse(RemoteMasterTransportPolicy.tryDirectRecovery(false,true));
 }
 @Test public void streamProgressExtendsIdleDeadlineButNotHardLimit(){
  assertFalse(RemoteMasterTransportPolicy.expired(1000L,175000L,190000L));
  assertTrue(RemoteMasterTransportPolicy.expired(1000L,1000L,181000L));
  assertTrue(RemoteMasterTransportPolicy.expired(1000L,350000L,361000L));
 }
 @Test public void partialNarrationCannotBeCommitted(){
  assertFalse(RemoteMasterTransportPolicy.completeEnvelope("{\"narration\":\"Kvothe, você"));
  assertFalse(RemoteMasterTransportPolicy.completeEnvelope("{\"options\":[]}"));
  assertTrue(RemoteMasterTransportPolicy.completeEnvelope("{\"narration\":\"Kvothe, Kote responde.\"}"));
 }
 @Test public void textAndValidationAreNotQueuedBehindImageAudioOrIndex(){
  assertNotSame(AppExecutors.masterText(),AppExecutors.network());
  assertNotSame(AppExecutors.masterResponse(),AppExecutors.io());
  assertNotSame(AppExecutors.masterResponse(),AppExecutors.masterPreparation());
 }
}