package org.junit;
public class Assert {
 public static void assertEquals(Object a,Object b){if(!java.util.Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
 public static void assertEquals(String note,Object a,Object b){if(!java.util.Objects.equals(a,b))throw new AssertionError(note+": "+a+" != "+b);}
 public static void assertEquals(int a,int b){if(a!=b)throw new AssertionError(a+" != "+b);}
 public static void assertTrue(boolean x){if(!x)throw new AssertionError("expected true");}
 public static void assertFalse(boolean x){if(x)throw new AssertionError("expected false");}
}
