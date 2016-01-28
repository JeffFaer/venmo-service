package name.falgout.jeffrey.moneydance.venmoservice.rest;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.reflect.TypeToken;

public class VenmoResponse<T> {
  private final Optional<T> data;
  private final Optional<Pagination> pagination;
  private final Optional<VenmoException> exception;

  private TypeToken<T> dataType;

  VenmoResponse(@JsonProperty("data") T data, @JsonProperty("pagination") Pagination pagination,
      @JsonProperty("error") VenmoException error) {
    this.data = Optional.ofNullable(data);
    this.pagination = Optional.ofNullable(pagination);
    exception = Optional.ofNullable(error);
  }

  public T getData() throws VenmoException {
    return data.orElseThrow(exception::get);
  }

  public Optional<Pagination> getPagination() {
    return pagination;
  }

  public boolean hasNext() {
    return pagination.flatMap(Pagination::getNext).isPresent();
  }

  public boolean hasPrevious() {
    return pagination.flatMap(Pagination::getPrevious).isPresent();
  }

  public Optional<VenmoException> getException() {
    return exception;
  }

  TypeToken<T> getDataType() {
    return dataType;
  }

  void setDataType(TypeToken<T> dataType) {
    this.dataType = dataType;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((data == null) ? 0 : data.hashCode());
    result = prime * result + ((exception == null) ? 0 : exception.hashCode());
    result = prime * result + ((pagination == null) ? 0 : pagination.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof VenmoResponse)) {
      return false;
    }
    VenmoResponse<?> other = (VenmoResponse<?>) obj;
    if (data == null) {
      if (other.data != null) {
        return false;
      }
    } else if (!data.equals(other.data)) {
      return false;
    }
    if (exception == null) {
      if (other.exception != null) {
        return false;
      }
    } else if (!exception.equals(other.exception)) {
      return false;
    }
    if (pagination == null) {
      if (other.pagination != null) {
        return false;
      }
    } else if (!pagination.equals(other.pagination)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(getClass().getName());
    builder.append(" [");
    if (data.isPresent()) {
      builder.append(" data=");
      builder.append(data.get());
      if (pagination.isPresent()) {
        builder.append(", pagination=");
        builder.append(pagination.get());
      }
    }
    if (exception.isPresent()) {
      builder.append(" exception=");
      builder.append(exception.get());
    }
    builder.append("]");
    return builder.toString();
  }
}
