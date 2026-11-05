package systems.helius.reflet.accessors;

import org.junit.jupiter.api.Test;
import systems.helius.reflet.*;
import systems.helius.reflet.exceptions.AccessorException;

import java.lang.invoke.MethodHandles;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link FieldHandlesAccessor}.
 */
class FieldHandlesAccessorTest {

    /**
     * Verifies the accessor accepts any class.
     */
    @Test
    void GivenAnyClass_WhenAccepts_ThenReturnsTrue() {
        FieldHandlesAccessor accessor = new FieldHandlesAccessor(new ClassInspector(), new LookupManager());
        assertTrue(accessor.accepts(Object.class, null));
        assertTrue(accessor.accepts(ChildSample.class, null));
        assertTrue(accessor.accepts(int.class, null));
    }

    /**
     * Verifies extraction reads inherited instance fields while ignoring static and null-valued fields.
     */
    @Test
    void GivenInheritedObject_WhenExtract_ThenReturnsOnlyNonNullInstanceFields() throws AccessorException {
        FieldHandlesAccessor accessor = new FieldHandlesAccessor(new ClassInspector(), new LookupManager());
        ChildSample sample = new ChildSample();

        Collection<Content> extracted = accessor.extract(sample, null, newContext(accessor), new IntrospectionSettings());

        assertEquals(2, extracted.size());
        assertEquals(Set.of("child-value", 7), extracted.stream().map(Content::value).collect(java.util.stream.Collectors.toSet()));
        assertEquals(Set.of("childValue", "baseNumber"), extracted.stream().map(content -> content.holdingField().getName()).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void GivenUnreadableObject_WhenExtractWithSkipAccessDenialPolicy_ThenFindNothing() throws AccessorException {
        FieldHandlesAccessor accessor = new FieldHandlesAccessor(new ClassInspector(), new LookupManager());
        String unreadableObject = "unreadable";

        IntrospectionSettings settings = IntrospectionSettings.builder()
                .withAccessDenialPolicy(AccessDenialPolicy.SKIP)
                .build();

        Collection<Content> extracted = accessor.extract(unreadableObject, null, newContext(accessor), settings);

        assertTrue(extracted.isEmpty());
    }

    @Test
    void GivenUnreadableObject_WhenExtractWithFailPolicy_ThenThrowsFatal() {
        FieldHandlesAccessor accessor = new FieldHandlesAccessor(new ClassInspector(), new LookupManager());
        String unreadableObject = "unreadable";

        IntrospectionSettings settings = IntrospectionSettings.builder()
                .withAccessDenialPolicy(AccessDenialPolicy.FAIL)
                .build();

        try {
            accessor.extract(unreadableObject, null, newContext(accessor), settings);
            fail("Should have thrown an exception");
        } catch (AccessorException e) {
            assertTrue(e.isFatal());
        }
    }

    @Test
    void GivenObjectWithInaccessibleParent_WhenExtractWithSkipAccessDenialPolicy_ThenExtractOnlyReadableFields() throws AccessorException {
        FieldHandlesAccessor accessor = new FieldHandlesAccessor(new ClassInspector(), new LookupManager());
        var obj = new ObjectWithInaccessibleParent();

        IntrospectionSettings settings = IntrospectionSettings.builder()
                .withAccessDenialPolicy(AccessDenialPolicy.SKIP)
                .build();

        Collection<Content> extracted = accessor.extract(obj, null, newContext(accessor), settings);

        assertEquals(1, extracted.size());
        assertEquals("readable", extracted.iterator().next().value());
    }

    @Test
    void GivenObjectWithInaccessibleParent_WhenExtractWithFailPolicy_ThenThrowsFatal() {
        FieldHandlesAccessor accessor = new FieldHandlesAccessor(new ClassInspector(), new LookupManager());
        var obj = new ObjectWithInaccessibleParent();

        IntrospectionSettings settings = IntrospectionSettings.builder()
                .withAccessDenialPolicy(AccessDenialPolicy.FAIL)
                .build();

        try {
            accessor.extract(obj, null, newContext(accessor), settings);
            fail("Should have thrown an exception");
        } catch (AccessorException e) {
            assertTrue(e.isFatal());
        }
    }

    private static IntrospectionContext<Object> newContext(ContentAccessor accessor) {
        return new IntrospectionContext<>(
                Object.class,
                MethodHandles.lookup(),
                new HashSet<>(),
                Collections.newSetFromMap(new IdentityHashMap<>()),
                accessor
        );
    }

    /**
     * Sample base class used to validate hierarchical field extraction.
     */
    private static class BaseSample {
        @SuppressWarnings("unused")
        private final Integer baseNumber = 7;

        @SuppressWarnings("unused")
        private static final int BASE_STATIC = 999;
    }

    /**
     * Sample derived class used to validate static and null filtering.
     */
    private static class ChildSample extends BaseSample {
        @SuppressWarnings("unused")
        private final String childValue = "child-value";

        @SuppressWarnings("unused")
        private final String nullableValue = null;

        @SuppressWarnings("unused")
        private static final String DERIVED_STATIC = "ignored";
    }

    /**
     * Sample class with a parent in a module that is not open to this module.
     * <p>
     * Tests should only find the field declared in this class, and not the field declared in the parent class.
     */
    private static class ObjectWithInaccessibleParent extends ArrayList<Object> {
        private final String readableField = "readable"; //NOSONAR
    }
}