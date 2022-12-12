package com.lgt.cwm.ui.adapter.mapping;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class MappingModelList extends ArrayList<MappingModel<?>> {

    public MappingModelList() { }

    public MappingModelList(@NonNull Collection<? extends MappingModel<?>> c) {
        super(c);
    }

    public static @NonNull MappingModelList singleton(@NonNull MappingModel<?> model) {
        MappingModelList list = new MappingModelList();
        list.add(model);
        return list;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static @NonNull java.util.stream.Collector<MappingModel<?>, MappingModelList, MappingModelList> collect() {
        return new java.util.stream.Collector<MappingModel<?>, MappingModelList, MappingModelList>() {
            @Override
            public java.util.function.Supplier<MappingModelList> supplier() {
                return MappingModelList::new;
            }

            @Override
            public java.util.function.BiConsumer<MappingModelList, MappingModel<?>> accumulator() {
                return MappingModelList::add;
            }

            @Override
            public BinaryOperator<MappingModelList> combiner() {
                return (left, right) -> {
                    left.addAll(right);
                    return left;
                };
            }

            @Override
            public java.util.function.Function<MappingModelList, MappingModelList> finisher() {
                return java.util.function.Function.identity();
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Sets.immutableEnumSet(Characteristics.IDENTITY_FINISH);
            }
        };
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static @NonNull
    Collector<MappingModel<?>, MappingModelList, MappingModelList> toMappingModelList() {
        return new Collector<MappingModel<?>, MappingModelList, MappingModelList>() {
            @Override
            public @NonNull
            Supplier<MappingModelList> supplier() {
                return MappingModelList::new;
            }

            @Override
            public @NonNull
            BiConsumer<MappingModelList, MappingModel<?>> accumulator() {
                return MappingModelList::add;
            }

            @Override
            public BinaryOperator<MappingModelList> combiner() {
                return null;
            }

            @Override
            public @NonNull
            Function<MappingModelList, MappingModelList> finisher() {
                return mappingModels -> mappingModels;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return null;
            }
        };
    }
}

