package com.eshoppingzone.website.dto;

import java.util.ArrayList;
import java.util.List;

public class PagedResult<T> {
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private int totalElements;
    private int totalPages;
    private int startElement;
    private int endElement;
    private boolean hasPrevious;
    private boolean hasNext;
    private List<Integer> pageNumbers;

    public PagedResult() {
        this.content = new ArrayList<>();
        this.pageNumbers = new ArrayList<>();
    }

    public static <E> PagedResult<E> from(List<E> fullList, int page, int size) {
        if (fullList == null) {
            fullList = new ArrayList<>();
        }
        if (size <= 0) {
            size = 10;
        }
        int totalElements = fullList.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        if (totalPages == 0) {
            totalPages = 1;
        }

        if (page < 1) {
            page = 1;
        } else if (page > totalPages) {
            page = totalPages;
        }

        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<E> content;
        if (fromIndex >= totalElements || fromIndex < 0) {
            content = new ArrayList<>();
        } else {
            content = new ArrayList<>(fullList.subList(fromIndex, toIndex));
        }

        int startElement = totalElements == 0 ? 0 : fromIndex + 1;
        int endElement = totalElements == 0 ? 0 : toIndex;

        List<Integer> pageNumbers = new ArrayList<>();
        int startPage = Math.max(1, page - 2);
        int endPage = Math.min(totalPages, page + 2);
        for (int i = startPage; i <= endPage; i++) {
            pageNumbers.add(i);
        }

        PagedResult<E> result = new PagedResult<>();
        result.setContent(content);
        result.setPageNumber(page);
        result.setPageSize(size);
        result.setTotalElements(totalElements);
        result.setTotalPages(totalPages);
        result.setStartElement(startElement);
        result.setEndElement(endElement);
        result.setHasPrevious(page > 1);
        result.setHasNext(page < totalPages);
        result.setPageNumbers(pageNumbers);

        return result;
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(int totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getStartElement() {
        return startElement;
    }

    public void setStartElement(int startElement) {
        this.startElement = startElement;
    }

    public int getEndElement() {
        return endElement;
    }

    public void setEndElement(int endElement) {
        this.endElement = endElement;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public List<Integer> getPageNumbers() {
        return pageNumbers;
    }

    public void setPageNumbers(List<Integer> pageNumbers) {
        this.pageNumbers = pageNumbers;
    }
}
