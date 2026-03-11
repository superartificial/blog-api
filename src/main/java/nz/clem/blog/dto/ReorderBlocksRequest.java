package nz.clem.blog.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReorderBlocksRequest {

    private List<BlockOrder> blocks;

    @Data
    public static class BlockOrder {
        private Long id;
        private int sortOrder;
    }
}
